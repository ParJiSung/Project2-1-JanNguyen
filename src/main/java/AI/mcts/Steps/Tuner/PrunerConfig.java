package AI.mcts.Steps.Tuner;

public class PrunerConfig {
    public final double threshold;
    public final double centralityWeight;
    public final double connectivityWeight;
    public final double biasScale;
    public final double spWeight;
    public final double cExploration;

    public PrunerConfig(double threshold, double centralityWeight, double connectivityWeight, double biasScale, double spWeight, double cExploration) {
        this.threshold = threshold;
        this.centralityWeight = centralityWeight;
        this.connectivityWeight = connectivityWeight;
        this.biasScale = biasScale;
        this.spWeight = spWeight;
        this.cExploration = cExploration;
    }

    @Override
    public String toString() {
        return String.format(
            "thr=%.3f, cent=%.3f, conn=%.3f, bias=%.3f, sp=%.3f, c=%.3f",
            threshold, centralityWeight, connectivityWeight,
            biasScale, spWeight, cExploration
        );
    }
}
