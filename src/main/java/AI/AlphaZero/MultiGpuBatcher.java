package AI.AlphaZero;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.api.ndarray.INDArray;

// This class acts as a Load Balancer / Router for multiple GPUs.
// It effectively replaces the single NeuralNetBatcher.
public class MultiGpuBatcher implements Runnable, Batcher {
    
    // We maintain a list of workers (one per GPU)
    private final List<NeuralNetBatcher> workers = new ArrayList<>();
    private final List<AlphaZeroNet> workerNets = new ArrayList<>();
    private final AtomicInteger counter = new AtomicInteger(0);
    
    public MultiGpuBatcher(AlphaZeroNet masterNet, int batchSize) {
        int numDevices = 0;
        try {
            numDevices = Nd4j.getAffinityManager().getNumberOfDevices();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (numDevices < 2) {
             System.out.println("WARNING: AffinityManager reported fewer than 2 devices (" + numDevices + "). Forcing usage of 2 devices as requested.");
             numDevices = 2; 
        }
        
        System.out.println("DEBUG: MultiGpuBatcher detected " + numDevices + " devices via AffinityManager.");
        
        System.out.println("MultiGpuBatcher: Detected " + numDevices + " GPUs. Initializing workers...");
        
        for (int i = 0; i < numDevices; i++) {
            // 1. Create a NEW network instance for this GPU (Topology only)
            // We assume safe separation of memory.
            // Ideally we clone the master configuration.
            // Since AlphaZeroNet(size) creates a fresh initialized net, that's fine.
            // We will overwrite weights immediately.
            AlphaZeroNet childNet = new AlphaZeroNet(masterNet.getBoardSize()); 
            
            // 2. Create the worker (pinned to device i)
            NeuralNetBatcher worker = new NeuralNetBatcher(childNet, batchSize, i);
            
            workers.add(worker);
            workerNets.add(childNet);
            System.out.println("  -> Worker " + i + " created.");
        }
        
        // Initial sync to ensure they start with master weights
        updateWeights(masterNet);
    }

    /**
     * Distributes the prediction request to one of the workers.
     * User Requested: Round-Robin using AtomicInteger.
     */
    public CompletableFuture<NeuralNetBatcher.Output> predict(float[] input) {
        // Round-Robin: 0, 1, 0, 1... mechanism
        int idx = Math.abs(counter.getAndIncrement() % workers.size());
        return workers.get(idx).predict(input);
    }
    
    public void updateWeights(AlphaZeroNet master) {
        System.out.println(">>> Broadcasting weights to " + workers.size() + " GPUs...");
        for (NeuralNetBatcher worker : workers) {
             worker.updateModelWeights(master);
        }
    }
    
    public int getBoardSize() {
        if (!workerNets.isEmpty()) return workerNets.get(0).getBoardSize();
        return 11; // Default
    }

    // Lifecycle methods delegate to all workers
    @Override
    public void run() {
        for (NeuralNetBatcher worker : workers) {
            Thread t = new Thread(worker);
            t.setDaemon(true);
            t.start();
        }
    }
    
    public void pause() { for (NeuralNetBatcher w : workers) w.pause(); }
    public void resume() { for (NeuralNetBatcher w : workers) w.resume(); }
    public void stop() { for (NeuralNetBatcher w : workers) w.stop(); }
}
