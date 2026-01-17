package AI.AlphaZero;

public class TrainingExampleData {
    public float[] inputBoard;  // Was INDArray
    public float[] targetPolicy; // Was INDArray
    public float[] targetValue;  // Was INDArray (or scalar)

    public TrainingExampleData(float[] input, float[] policy, float value) {
        this.inputBoard = input;
        this.targetPolicy = policy;
        this.targetValue = new float[]{ value };
    }
}