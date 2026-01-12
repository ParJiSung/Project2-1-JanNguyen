package AI.mcts.Steps.Tuner;

public class TunerTester {
    public static void main(String[] args) {
        int boardSize    = 7;
        int iterations   = 1000;
        int gamesPerSide = 50;
        MCTSTuner tuner = new MCTSTuner(boardSize, iterations, gamesPerSide);
        double[] cValues = {
                Math.sqrt(2),
                2.0
        };
        int trialsPerC = 10;
        tuner.randomSearchPerC(cValues, trialsPerC);
        // tuner.randomSearch(10);
    }
}
