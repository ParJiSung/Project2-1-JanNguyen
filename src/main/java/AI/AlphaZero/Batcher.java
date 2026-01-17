package AI.AlphaZero;

import java.util.concurrent.CompletableFuture;

public interface Batcher {
    CompletableFuture<NeuralNetBatcher.Output> predict(float[] input);
}
