package AI.AlphaZero;

import org.nd4j.linalg.api.ndarray.INDArray;

/**
 * Represents one training sample: (Board State) -> (Target Policy, Target Value)
 */
public class TrainingExampleData {
    INDArray inputBoard; // The 3-plane tensor
    INDArray targetPolicy; // The probabilities from MCTS
    INDArray targetValue; // The actual winner (-1 or 1)

    public TrainingExampleData(INDArray inputBoard, INDArray targetPolicy, double value) {
        this.inputBoard = inputBoard;
        this.targetPolicy = targetPolicy;
        // ND4J expects the value as an array, even if it's a single number. Convert it accordingly.
        this.targetValue = org.nd4j.linalg.factory.Nd4j.create(new double[]{value}, new int[]{1, 1});
    }
}