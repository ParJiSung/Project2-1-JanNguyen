
import org.junit.jupiter.api.Test;

import AI.mcts.Steps.Tuner.PrunerConfig;

import static org.junit.jupiter.api.Assertions.*;

class PrunerConfigTest {

    @Test
    void prunerConfigTest() {
        PrunerConfig cfg = new PrunerConfig(1.23456, 2.0, 3.0, 4.0, 5.0, 6.0);

        assertEquals(1.23456, cfg.threshold, 0.0);
        assertEquals(2.0, cfg.centralityWeight, 0.0);
        assertEquals(3.0, cfg.connectivityWeight, 0.0);
        assertEquals(4.0, cfg.biasScale, 0.0);
        assertEquals(5.0, cfg.spWeight, 0.0);
        assertEquals(6.0, cfg.cExploration, 0.0);
    }


    @Test
    void toStringTestHandlesNaN() {
        PrunerConfig cfg = new PrunerConfig(Double.NaN, 1.0, 2.0, 3.0, 4.0, 5.0);

        String s = cfg.toString();
        assertTrue(s.contains("thr=NaN"), "Expected NaN to appear in formatted string");
    }

    @Test
    void toStringTest_handlesInfinity() {
        PrunerConfig cfg1 = new PrunerConfig(Double.POSITIVE_INFINITY, 1.0, 2.0, 3.0, 4.0, 5.0);
        PrunerConfig cfg2 = new PrunerConfig(Double.NEGATIVE_INFINITY, 1.0, 2.0, 3.0, 4.0, 5.0);

        assertTrue(cfg1.toString().contains("thr=Infinity"), "Expected Infinity to appear in formatted string");
        assertTrue(cfg2.toString().contains("thr=-Infinity"), "Expected -Infinity to appear in formatted string");
    }

    @Test
    void toStringTest_producesStablePrefixLabels() {
        PrunerConfig cfg = new PrunerConfig(0, 0, 0, 0, 0, 0);

        String s = cfg.toString();
        assertTrue(s.startsWith("thr="));
        assertTrue(s.contains(", cent="));
        assertTrue(s.contains(", conn="));
        assertTrue(s.contains(", bias="));
        assertTrue(s.contains(", sp="));
        assertTrue(s.contains(", c="));
    }
    @Test
    void immutabilityTest() {
        PrunerConfig cfg = new PrunerConfig(1, 2, 3, 4, 5, 6);

        assertEquals(1.0, cfg.threshold, 0.0);
        assertEquals(2.0, cfg.centralityWeight, 0.0);
        assertEquals(3.0, cfg.connectivityWeight, 0.0);
        assertEquals(4.0, cfg.biasScale, 0.0);
        assertEquals(5.0, cfg.spWeight, 0.0);
        assertEquals(6.0, cfg.cExploration, 0.0);
    }

}
