package AI.AlphaZero;

public class AlphaZeroRunner {
    public static void main(String[] args) {
        // Train on a board (specify the size in the constructor, I do not specify it here because this allows us to be a bit more flexible during testing without having to change the comments every time)
        AlphaZeroTrainer trainer = new AlphaZeroTrainer(11);
        
        // TODO: Decide on the number of self-play games, batch size, and MCTS iterations per move.
        // Run self-play games, with a number MCTS simulations per move to be specified below.
        trainer.train(100_000, 200);
    }
}