package AI.AlphaZero;

public class AlphaZeroRunner {
    public static void main(String[] args) {
    // Shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread(() -> System.out.println("Shutting down...")));

    System.out.println(">>> 1. Initializing...");
    AlphaZeroTrainer trainer = new AlphaZeroTrainer(11); // 11
    
    System.out.println(">>> 2. Running Smoke Test (Fast)...");
    
    int games = 50_000;
    int iterations = 50;
    
    // Robust argument parsing: find first two integers, ignore others (like JVM flags)
    int integersFound = 0;
    for (String arg : args) {
        try {
            int val = Integer.parseInt(arg);
            if (integersFound == 0) {
                games = val;
                integersFound++;
            } else if (integersFound == 1) {
                iterations = val;
                integersFound++;
            }
        } catch (NumberFormatException e) {
            // Ignore non-integer arguments
        }
    }
    
    System.out.println("Running " + games + " games with " + iterations + " iterations.");
    trainer.train(games, iterations);
    
    System.out.println(">>> 3. Finished.");
}
}