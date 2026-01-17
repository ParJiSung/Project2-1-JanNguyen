package AI.AlphaZero;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class NeuralNetBatcher implements Runnable, Batcher {
    private final AlphaZeroNet network;
    private final BlockingQueue<Request> inputQueue;
    private final BlockingQueue<BatchJob> gpuQueue; 
    
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final int maxBatchSize;
    private final int deviceIndex; // New: Device to pin this batcher to
    
    // ... constants ...

    public void updateModelWeights(AlphaZeroNet master) {
        // Pause first to ensure no inference is running
        pause();
        try {
            // Simple parameter copy
            this.network.getModel().setParams(master.getModel().params().dup());
        } catch (Exception e) {
            e.printStackTrace();
        }
        resume();
    }
    
    private static final long MAX_WAIT_NANOS = 5_000_000; // 5ms
    private final AtomicLong totalSamplesProcessed = new AtomicLong(0);

    public NeuralNetBatcher(AlphaZeroNet network, int maxBatchSize, int deviceIndex) {
        this.network = network;
        this.maxBatchSize = Math.min(Math.max(maxBatchSize, 256), 8192); // Cap at 8192
        this.deviceIndex = deviceIndex;
        this.inputQueue = new LinkedBlockingQueue<>((int)(this.maxBatchSize * 1.5));
        this.gpuQueue = new LinkedBlockingQueue<>(2);
    }

    public CompletableFuture<Output> predict(float[] input) {
        CompletableFuture<Output> future = new CompletableFuture<>();
        try {
            inputQueue.put(new Request(input, future));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return future;
    }

    @Override
    public void run() {
        startHeartbeat();
        Thread builderThread = new Thread(this::runBatchBuilder, "Batch-Builder");
        builderThread.setDaemon(true);
        builderThread.start();
        runGpuInference(); 
    }

    private void runBatchBuilder() {
        List<Request> batchBuffer = new ArrayList<>(maxBatchSize);
        while (running.get()) {
            try {
                while (paused.get() && running.get()) Thread.sleep(10);

                Request first = inputQueue.take();
                batchBuffer.add(first);
                long start = System.nanoTime();
                while (batchBuffer.size() < maxBatchSize && (System.nanoTime() - start) < MAX_WAIT_NANOS) {
                    Request next = inputQueue.poll();
                    if (next != null) batchBuffer.add(next);
                    else Thread.onSpinWait(); 
                }

                BatchJob job = prepareBatch(new ArrayList<>(batchBuffer));
                batchBuffer.clear();
                gpuQueue.put(job);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private BatchJob prepareBatch(List<Request> requests) {
        int batchSize = requests.size();
        int singleInputLength = requests.get(0).inputData.length;
        int side = (int) Math.sqrt(singleInputLength / 3);

        float[] batchBuffer = new float[batchSize * singleInputLength];
        for (int i = 0; i < batchSize; i++) {
            System.arraycopy(requests.get(i).inputData, 0, batchBuffer, i * singleInputLength, singleInputLength);
        }
        // Native allocation (Must be closed later!)
        INDArray inputTensor = Nd4j.create(batchBuffer, new int[]{batchSize, 3, side, side});
        return new BatchJob(inputTensor, requests);
    }

    private void runGpuInference() {
        // Pin this thread to the specific device
        try {
            // Attempt logging to verify this runs
            System.out.println("DEBUG: Batcher for device " + deviceIndex + " starting on thread " + Thread.currentThread().getName());
            
            // Try explicit pointer setting. Note: unsafeSetDevice is often available where attach is not.
            Nd4j.getAffinityManager().unsafeSetDevice(deviceIndex);
            System.out.println("DEBUG: Successfully called unsafeSetDevice(" + deviceIndex + ")");
        } catch (Exception e) {
            System.err.println("ERROR: Failed to set device affinity: " + e.getMessage());
            System.err.println("WARNING: Worker for device " + deviceIndex + " will run on the DEFAULT device (likely 0).");
            // e.printStackTrace(); // Optional: reduce noise since we expect this now
        }
        
        while (running.get()) {
            try {
                BatchJob job = gpuQueue.poll(1, TimeUnit.SECONDS);
                if (job == null) continue;

                // 1. INFERENCE
                INDArray[] results = network.getModel().output(job.input);
                
                // 2. IMMEDIATE EXTRACTION & CLEANUP
                // Extract data to Java Heap (GC safe)
                INDArray policyBatch = results[0];
                INDArray valueBatch = results[1];
                
                float[][] policies = policyBatch.toFloatMatrix();
                double[] values = valueBatch.toDoubleVector();

                // *** CRITICAL: Close Native Pointers Immediately ***
                policyBatch.close();
                valueBatch.close();
                job.input.close(); // Close the input batch we created
                
                // 3. DISTRIBUTE (Safe Java Objects)
                int currentBatchSize = job.requests.size();
                IntStream.range(0, currentBatchSize).parallel().forEach(i -> {
                    job.requests.get(i).future.complete(new Output(policies[i], values[i]));
                });

                long processed = totalSamplesProcessed.addAndGet(currentBatchSize);
                
                // PERIODIC CLEANUP: Force GC to reclaim GPU memory from deleted INDArrays
                if ((processed / maxBatchSize) % 50 == 0) { // Every ~50 batches
                     Nd4j.getMemoryManager().invokeGc(); 
                     // System.gc(); // Optional, heavier
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public void pause() {
        paused.set(true);
        while (!inputQueue.isEmpty() || !gpuQueue.isEmpty()) try { Thread.sleep(50); } catch (Exception e) {}
    }
    public void resume() { paused.set(false); }
    public void stop() { running.set(false); }

    private void startHeartbeat() {
        Thread monitor = new Thread(() -> {
            long lastCount = 0;
            long lastTime = System.currentTimeMillis();
            while (running.get()) {
                try {
                    Thread.sleep(5000); 
                    long currentCount = totalSamplesProcessed.get();
                    System.out.println(String.format("HEARTBEAT: Processed %d. InQ: %d, GpuQ: %d", currentCount, inputQueue.size(), gpuQueue.size()));
                    lastCount = currentCount;
                } catch (InterruptedException e) { break; }
            }
        });
        monitor.setDaemon(true);
        monitor.start();
    }

    private static class Request {
        float[] inputData;
        CompletableFuture<Output> future;
        Request(float[] d, CompletableFuture<Output> f) { inputData = d; future = f; }
    }
    private static class BatchJob {
        INDArray input;
        List<Request> requests;
        BatchJob(INDArray i, List<Request> r) { input = i; requests = r; }
    }
    
    // UPDATED: Now uses standard Java arrays, NOT INDArray
    public static class Output {
        public float[] policy; 
        public double value;
        public Output(float[] p, double v) { policy = p; value = v; }
    }
}