package AI.mcts.Steps.Tuner;

import Game.Player;
import java.util.Random;
import AI.AiPlayer.AIAgent;
import AI.AiPlayer.AITester;
import AI.AiPlayer.MCTSPlayer;

/**
 * Simple hyperparameter tuner for the MCTS MovePruner heuristics.
 *
 * It can:
 *  - sample full random configs (including cExploration), or
 *  - for each fixed cExploration, sample random (threshold, centralityWeight,
 *    connectivityWeight, biasScale, spWeight) and find the best combo.
 */
public final class MCTSTuner {

    private final int boardSize;
    private final int iterations;
    private final int gamesPerSide;
    private final Random rng;

    public MCTSTuner(int boardSize, int iterations, int gamesPerSide, long seed) {
        this.boardSize = boardSize;
        this.iterations = iterations;
        this.gamesPerSide = gamesPerSide;
        this.rng = new Random(seed);
    }

    public MCTSTuner(int boardSize, int iterations, int gamesPerSide) {
        this(boardSize, iterations, gamesPerSide, System.currentTimeMillis());
    }

    /**
     * Full random config, including cExploration.
     * (Keep if you still want a completely unconstrained search.)
     */
    private PrunerConfig sampleRandomConfig() {
        double threshold = 0.7 + rng.nextDouble() * 0.3; // [0.7, 1.0]
        double centralityWeight = rng.nextDouble();      // [0.0, 1.0]
        double connectivityWeight = 1.0 - centralityWeight;
        double biasScale = 0.02 + rng.nextDouble() * 0.06; // [0.02, 0.08]
        double spWeight  = rng.nextDouble() * 0.2;         // [0.0, 0.2]
        double cExploration = 0.5 + rng.nextDouble() * 1.5; // [0.5, 2.0]
        return new PrunerConfig( threshold, centralityWeight, connectivityWeight, biasScale, spWeight, cExploration);
    }

    /**
     * Random config for a fixed cExploration.
     * cExploration is given, everything else is sampled.
     */
    private PrunerConfig sampleRandomConfigForC(double cExploration) {
        double threshold = 0.3 + rng.nextDouble() * 0.7; // [0.3, 1.0]
        double centralityWeight = rng.nextDouble();      // [0.0, 1.0]
        double connectivityWeight = 1.0 - centralityWeight;
        double biasScale = 0.02 + rng.nextDouble() * 0.06; // [0.02, 0.08]
        double spWeight = rng.nextDouble() * 0.2;         // [0.0, 0.2]
        return new PrunerConfig(threshold, centralityWeight, connectivityWeight, biasScale, spWeight, cExploration);
    }

    /**
     * Evaluate a single configuration by playing a color-swapped mini-tournament
     * between base MCTS and optimized MCTS.
     */
    public double evaluateConfig(PrunerConfig cfg, boolean printMatches) {
        AIAgent baseRed = new MCTSPlayer(Player.RED, iterations);
        AIAgent optBlack = new MCTSPlayer(Player.BLACK, iterations, cfg.threshold, cfg.centralityWeight, cfg.connectivityWeight, cfg.biasScale, cfg.spWeight, cfg.cExploration);
        AITester.TestResult result1 = AITester.runMatch(baseRed, optBlack, gamesPerSide, boardSize, printMatches);
        AIAgent optRed = new MCTSPlayer(Player.RED, iterations, cfg.threshold, cfg.centralityWeight, cfg.connectivityWeight, cfg.biasScale, cfg.spWeight, cfg.cExploration);
        AIAgent baseBlack = new MCTSPlayer(Player.BLACK, iterations);
        AITester.TestResult result2 = AITester.runMatch(optRed, baseBlack, gamesPerSide, boardSize, printMatches);
        int optWins = result1.blackWins + result2.redWins;
        int totalGames = result1.totalGames + result2.totalGames;
        return (totalGames > 0) ? (optWins * 100.0 / totalGames) : 0.0;
    }

    /**
     * Original global random search (including cExploration).
     * You can keep this if you still want to explore everything at once.
     */
    public void randomSearch(int trials) {
        PrunerConfig bestCfg = null;
        double bestWinRate = -1.0;
        for (int i = 0; i < trials; i++) {
            PrunerConfig cfg = sampleRandomConfig();
            System.out.printf("Trial %d/%d: %s%n", i + 1, trials, cfg);
            double winRate = evaluateConfig(cfg, false);
            System.out.printf("  => optimized win rate vs base: %.2f%%%n", winRate);
            if (winRate > bestWinRate) {
                bestWinRate = winRate;
                bestCfg = cfg;
                System.out.printf("  NEW BEST! (winRate=%.2f%%)%n", bestWinRate);
            }
        }

        if (bestCfg != null) {
            System.out.println("\n==== BEST CONFIG FOUND ====");
            System.out.printf("  %s, winRate=%.2f%%%n", bestCfg, bestWinRate);
        } else {
            System.out.println("\nNo valid configs evaluated.");
        }
    }

    public void randomSearchPerC(double[] cValues, int trialsPerC) {
        PrunerConfig globalBestCfg = null;
        double globalBestWinRate = -1.0;
        for (double c : cValues) {
            System.out.printf("%n##### Tuning for c = %.5f #####%n", c);
            PrunerConfig bestCfgForC = null;
            double bestWinRateForC = -1.0;
            for (int i = 0; i < trialsPerC; i++) {
                PrunerConfig cfg = sampleRandomConfigForC(c);
                System.out.printf("  Trial %d/%d (c=%.5f): %s%n",
                        i + 1, trialsPerC, c, cfg);
                double winRate = evaluateConfig(cfg, false);
                System.out.printf("    => optimized win rate vs base: %.2f%%%n", winRate);
                if (winRate > bestWinRateForC) {
                    bestWinRateForC = winRate;
                    bestCfgForC = cfg;
                    System.out.printf("    NEW BEST FOR c=%.5f! (winRate=%.2f%%)%n",
                            c, bestWinRateForC);
                }
            }
            if (bestCfgForC != null) {
                System.out.printf("%n== Best config for c=%.5f ==%n", c);
                System.out.printf("  %s, winRate=%.2f%%%n%n",
                        bestCfgForC, bestWinRateForC);
            } else {
                System.out.printf("%nNo valid configs evaluated for c=%.5f%n", c);
            }
            if (bestWinRateForC > globalBestWinRate) {
                globalBestWinRate = bestWinRateForC;
                globalBestCfg = bestCfgForC;
            }
        }

        if (globalBestCfg != null) {
            System.out.println("\n==== GLOBAL BEST CONFIG ACROSS ALL c ====");
            System.out.printf("  %s, winRate=%.2f%%%n",
                    globalBestCfg, globalBestWinRate);
        }
    }
}
