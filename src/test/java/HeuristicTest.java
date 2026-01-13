
import AI.mcts.HexGame.GameState;
import AI.mcts.HexGame.Move;
import AI.mcts.Optimazation.Heuristic.Heuristic;
import Game.Board;
import Game.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class HeuristicTest {

    @Test
    void interfaceStructureTest_isInterface() {
        assertTrue(Heuristic.class.isInterface(), "Heuristic must be an interface");
        assertTrue(Modifier.isPublic(Heuristic.class.getModifiers()), "Heuristic should be public");
    }

    @Test
    void scoreMethodSignatureTest_existsAndMatches() throws NoSuchMethodException {
        Method m = Heuristic.class.getMethod("score", GameState.class, Move.class);

        assertEquals(double.class, m.getReturnType(), "score must return double");
        assertTrue(Modifier.isPublic(m.getModifiers()), "score must be public");
        assertTrue(Modifier.isAbstract(m.getModifiers()), "score must be abstract (interface method)");
    }

    @Test
    void polymorphismContractTest_canCallImplementationViaInterface() {
        
        Heuristic h = (state, move) -> 42.0;

        
        Board b = new Board(1);
        GameState s = new GameState(b, Player.RED);
        Move m = new Move(0, 0);

        assertEquals(42.0, h.score(s, m), 0.0);
    }
}

