package AI.AlphaZero;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.MultiDataSet;
import org.nd4j.linalg.factory.Nd4j;

import AI.mcts.HexGame.Move; 
import AI.mcts.Node;
import Game.Board;
import Game.Color;

public class AlphaZeroTrainer {
    private AlphaZeroNet network;
    private int boardSize;

    // CONFIGURATION - Optimized for dual A100 + 256 threads (respectful usage)
    // We use Virtual Threads (Java 21), so we can spawn thousands of lightweight threads.
    // This allows us to run many games concurrently to fill the massive GPU batch size.
    
    // A100 is huge. Let's aim for a batch size of 16384 to really saturate it and hide CPU latency.
    private static final int BATCH_SIZE = 8192; 
    
    // OVERSUBSCRIPTION:
    // We need massive concurrency to keep the queue full for such a huge batch size.
    // We will run 50,000 games in parallel.
    private static final int PLAY_BATCH_SIZE = 5000;

    private static final int TRAINING_EPOCHS = 3; 

    public AlphaZeroTrainer(int boardSize) {
        this.boardSize = boardSize;
        
        System.out.println("Backend: " + Nd4j.getBackend().getClass().getSimpleName());
        try {
            int numDevices = Nd4j.getAffinityManager().getNumberOfDevices();
            System.out.println("Available GPU devices: " + numDevices);
        } catch (Exception e) {
            System.out.println("GPU device info not available");
        }
        
        this.network = new AlphaZeroNet(boardSize);
        
        // A100 has 80GB.
        Nd4j.getMemoryManager().setAutoGcWindow(5000); 
    }

    public void train(int totalGames, int mctsIterations) {
        // Calculate how many "Generations" (Play Batches) we need
        // Each generation runs PLAY_BATCH_SIZE games in parallel
        
        int gamesToRun = totalGames;
        
        // Split into Play Batches (Generations)
        int numBatches = (int) Math.ceil((double) gamesToRun / PLAY_BATCH_SIZE);

        System.out.println("======================================================================");
        System.out.println("               ALPHA ZERO TRAINING SESSION (OPTIMIZED)");
        System.out.println("======================================================================");
        System.out.println("Target Total Games: " + gamesToRun);
        System.out.println("Batch Size (GPU):   " + BATCH_SIZE);
        System.out.println("Concurrency:        " + PLAY_BATCH_SIZE + " (Oversubscribed)");
        System.out.println("Generations:        " + numBatches);

        MultiGpuBatcher batcher = new MultiGpuBatcher(network, BATCH_SIZE);
        Thread batcherThread = new Thread(batcher);
        batcherThread.setDaemon(true);
        batcherThread.start();

        // Use Virtual Thread Executor
        // This is the MAGIC key to performance here.
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            
            for (int b = 0; b < numBatches; b++) {
                // Determine how many games to run in this generation
                int gamesInThisGeneration = Math.min(PLAY_BATCH_SIZE, gamesToRun - (b * PLAY_BATCH_SIZE));
                if (gamesInThisGeneration <= 0) break;

                AtomicInteger completedGames = new AtomicInteger(0);
                List<java.util.concurrent.Callable<List<TrainingExampleData>>> tasks = new ArrayList<>();
                
                final int generationIdx = b + 1;
                final int totalGamesInGen = gamesInThisGeneration;

                for (int i = 0; i < gamesInThisGeneration; i++) {
                    tasks.add(() -> {
                        // Cast or access facade if needed, but MultiGpuBatcher.predict matches signature of what MCTS needs? 
                        // MCTS expects a NeuralNetBatcher type probably?
                        // Let's check AlphaZeroMCTS constructor signature.
                        // If it expects NeuralNetBatcher, we might have a problem if MultiGpuBatcher is not a subclass.
                        // Wait, MultiGpuBatcher implements Runnable but is NOT a NeuralNetBatcher.
                        // I need to check AlphaZeroMCTS.
                        AlphaZeroMCTS localMcts = new AlphaZeroMCTS(batcher, boardSize);
                        List<TrainingExampleData> result = selfPlay(localMcts, mctsIterations);
                        int done = completedGames.incrementAndGet();
                        if (done % 500 == 0 || done == totalGamesInGen) {
                            System.out.println(String.format("    [Gen %d] Game %5d/%d finished.", generationIdx, done, totalGamesInGen));
                        }
                        return result;
                    });
                }

                System.out.println(">>> Starting Generation " + generationIdx + " with " + gamesInThisGeneration + " concurrent games...");
                List<java.util.concurrent.Future<List<TrainingExampleData>>> futures = executor.invokeAll(tasks);
                
                List<TrainingExampleData> batchExamples = new ArrayList<>();
                for (var future : futures) {
                    batchExamples.addAll(future.get()); // Collect results
                }

                System.out.println(">>> Generation finished. Pausing batcher for training...");
                batcher.pause();
                
                System.out.println(">>> Training Network on " + batchExamples.size() + " positions...");
                trainNetwork(batchExamples);
                
                // Update worker GPUs with new weights
                batcher.updateWeights(network);

                batcher.resume();
                try { network.save("hex_model_latest.zip"); } catch (Exception e) {}
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            batcher.stop();
        }
    }

    private List<TrainingExampleData> selfPlay(AlphaZeroMCTS localMcts, int iterations){
        List<TrainingExampleData> gameHistory = new ArrayList<>();
        Board board = new Board(boardSize);
        Color currentPlayer = Color.RED;

        while (!board.isTerminal()) {
            Node root = localMcts.search(board, currentPlayer, iterations);
            double temp = 1.0; 
            double[] policy = localMcts.getSearchPolicy(root, temp);

            // OPTIMIZATION: Store raw floats (Java Heap), NOT INDArrays (Native Heap)
            float[] encodedData = (root.cachedEncoding != null) ? root.cachedEncoding : BoardEncoder.encode(board, currentPlayer);
            
            // Convert double[] policy to float[] for storage
            float[] policyFloat = new float[policy.length];
            for(int i=0; i<policy.length; i++) policyFloat[i] = (float)policy[i];

            gameHistory.add(new TrainingExampleData(encodedData, policyFloat, 0.0f));

            Move bestMove = selectMoveFromPolicy(policy, board);
            if (currentPlayer == Color.RED) board.getMoveRed(bestMove.row, bestMove.col, null);
            else board.getMoveBlack(bestMove.row, bestMove.col, null);
            
            currentPlayer = (currentPlayer == Color.RED) ? Color.BLACK : Color.RED;
        }

        double result = board.redWins() ? 1.0 : (board.blackWins() ? -1.0 : 0.0);
        Color historyPlayer = Color.RED; 
        for (TrainingExampleData example : gameHistory) {
            float val = (float) ((historyPlayer == Color.RED) ? result : -result);
            example.targetValue[0] = val;
            historyPlayer = (historyPlayer == Color.RED) ? Color.BLACK : Color.RED;
        }
        return gameHistory;
    }

    private Move selectMoveFromPolicy(double[] policy, Board board) {
        double randomNumber = Math.random();
        double sum = 0;
        int selectedIdx = -1;
        int policyLength = policy.length;
        
        for (int i = 0; i < policyLength; i++) {
            sum += policy[i];
            if (randomNumber <= sum) {
                selectedIdx = i;
                break;
            }
        }
        if (selectedIdx == -1) {
            for (int i = 0; i < policyLength; i++) {
                if (policy[i] > 0) {
                    selectedIdx = i;
                    break;
                }
            }
        }
        int row = selectedIdx / boardSize;
        int col = selectedIdx % boardSize;
        
        return Move.get(row, col);
    }

    private void trainNetwork(List<TrainingExampleData> examples) {
        if (examples.isEmpty()) return;
        java.util.Collections.shuffle(examples);

        int totalExamples = examples.size();
        int miniBatchSize = 4096; // 4096 is safer for stability

        for (int epoch = 0; epoch < TRAINING_EPOCHS; epoch++) {
            for (int i = 0; i < totalExamples; i += miniBatchSize) {
                int end = Math.min(i + miniBatchSize, totalExamples);
                List<TrainingExampleData> batch = examples.subList(i, end);
                int currentBatchSize = batch.size();

                // CONVERT TO INDARRAY JUST IN TIME (And then discard)
                // 1. Flatten data into large buffers
                int inputSize = batch.get(0).inputBoard.length;
                int policySize = batch.get(0).targetPolicy.length;
                int side = (int)Math.sqrt(inputSize / 3);

                float[] inputsBuffer = new float[currentBatchSize * inputSize];
                float[] policiesBuffer = new float[currentBatchSize * policySize];
                float[] valuesBuffer = new float[currentBatchSize];

                for (int k = 0; k < currentBatchSize; k++) {
                    System.arraycopy(batch.get(k).inputBoard, 0, inputsBuffer, k*inputSize, inputSize);
                    System.arraycopy(batch.get(k).targetPolicy, 0, policiesBuffer, k*policySize, policySize);
                    valuesBuffer[k] = batch.get(k).targetValue[0];
                }

                // 2. Create NDArrays
                INDArray inputND = Nd4j.create(inputsBuffer, new int[]{currentBatchSize, 3, side, side});
                INDArray policyND = Nd4j.create(policiesBuffer, new int[]{currentBatchSize, policySize});
                INDArray valueND = Nd4j.create(valuesBuffer, new int[]{currentBatchSize, 1});

                try {
                    network.getModel().fit(new MultiDataSet(new INDArray[]{inputND}, new INDArray[]{policyND, valueND}));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // 3. CLOSE IMMEDIATELY to free GPU memory
                    inputND.close();
                    policyND.close();
                    valueND.close();
                }
            }
        }
    }
}