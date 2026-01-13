

import AI.mcts.Node;
import AI.mcts.HexGame.Move;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NodeTest {

    @Test
    void constructorTests() {
        Move m = new Move(2, 3);
        Node parent = new Node(null, null, 1);

        Node n = new Node(m, parent, 2);

        //Here we assign parameters from constructor
        assertSame(m, n.move);
        assertSame(parent, n.parent);
        assertEquals(2, n.playerThatMoved);

        assertNotNull(n.children, "children map must be created");
        assertTrue(n.children.isEmpty(), "children must start empty");

        assertEquals(0, n.visits, "visits must start at 0");
        assertEquals(0.0, n.wins, 0.0, "wins must start at 0.0");
        assertEquals(0.0, n.heuristicBias, 0.0, "heuristicBias must start at 0.0");
        assertEquals(0.0, n.priorProbability, 0.0, "priorProbability must default to 0.0 for basic ctor");
    }

    @Test
    void constructorWithDefault() {
        Move m = new Move(0, 0);
        Node parent = new Node(null, null, 1);

        Node n = new Node(m, parent, 2, 0.75);

        assertSame(m, n.move);
        assertSame(parent, n.parent);
        assertEquals(2, n.playerThatMoved);

        assertNotNull(n.children);
        assertTrue(n.children.isEmpty());

        assertEquals(0, n.visits);
        assertEquals(0.0, n.wins, 0.0);
        assertEquals(0.0, n.heuristicBias, 0.0);
        assertEquals(0.75, n.priorProbability, 0.0);
    }

    @Test
    void testNullMoveNullParent() {
        Node root = new Node(null, null, 1);

        assertNull(root.move);
        assertNull(root.parent);
        assertEquals(1, root.playerThatMoved);

        assertNotNull(root.children);
        assertTrue(root.children.isEmpty());
    }

    @Test
    void childrenMapTest() {
        Node a = new Node(null, null, 1);
        Node b = new Node(null, null, 2);

        Move m = new Move(1, 1);
        a.children.put(m, new Node(m, a, 1));

        assertEquals(1, a.children.size());
        assertEquals(0, b.children.size(), "each node must have its own children map instance");
    }

    @Test
    void retrievingChildrenTest() {
        Node parent = new Node(null, null, 1);

        Move key = new Move(4, 5);
        Node child = new Node(key, parent, 1);

        parent.children.put(key, child);

        assertEquals(1, parent.children.size());
        assertSame(child, parent.children.get(key));

        // Testing realisation of eguals/hashCode
        Move equivalentKey = new Move(4, 5);
        Node retrieved = parent.children.get(equivalentKey);

        //if equals/hashCode doesn't work correctly null will be returned
        assertSame(child, retrieved, "Check equals/hashCode!!!");
    }
    //Test checks whether children in Node class return null value
    @Test
    void childrenNullKeyNullValueTest() {
        Node parent = new Node(null, null, 1);

        parent.children.put(null, null);

        assertTrue(parent.children.containsKey(null));
        assertNull(parent.children.get(null));
        assertEquals(1, parent.children.size());
    }

    @Test
    void settingPublicFieldsTest() {
        Node n = new Node(null, null, 1);

        n.visits = 10;
        n.wins = 6.5;
        n.heuristicBias = -0.2;
        n.priorProbability = 0.33;

        assertEquals(10, n.visits);
        assertEquals(6.5, n.wins, 0.0);
        assertEquals(-0.2, n.heuristicBias, 0.0);
        assertEquals(0.33, n.priorProbability, 0.0);
    }

    @Test
    void unusualPlayerParametersTest() {
        Node n0 = new Node(null, null, 0);
        Node nNeg = new Node(null, null, -123);
        Node nBig = new Node(null, null, Integer.MAX_VALUE);

        assertEquals(0, n0.playerThatMoved);
        assertEquals(-123, nNeg.playerThatMoved);
        assertEquals(Integer.MAX_VALUE, nBig.playerThatMoved);

    }

    @Test
    void priorProbabilityTest() {
        Node neg = new Node(null, null, 1, -0.1);
        assertEquals(-0.1, neg.priorProbability, 0.0);

        Node nan = new Node(null, null, 1, Double.NaN);
        assertTrue(Double.isNaN(nan.priorProbability));

        Node inf = new Node(null, null, 1, Double.POSITIVE_INFINITY);
        assertTrue(Double.isInfinite(inf.priorProbability));
    }
}
