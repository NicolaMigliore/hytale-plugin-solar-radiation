package io.github.nicolamigliore.radiation;

import org.junit.jupiter.api.Test;
import static io.github.nicolamigliore.radiation.ShelterScan.Block.*;
import static org.junit.jupiter.api.Assertions.*;

class ShelterScanTest {
    @Test
    void configuredDepthChangesRequiredLayers() {
        assertEquals(Exposure.SHELTERED, ShelterScan.evaluate(0, 10, 1, y -> y == 5 ? SHIELD : PASS));
        assertEquals(Exposure.EXPOSED, ShelterScan.evaluate(0, 10, 4, y -> y < 3 ? SHIELD : PASS));
        assertThrows(IllegalArgumentException.class, () -> ShelterScan.evaluate(0, 10, 0, y -> PASS));
    }

    @Test
    void twoBlocksDoNotShelterButThreeSeparatedLayersDo() {
        assertEquals(Exposure.EXPOSED, ShelterScan.evaluate(1, 10, y -> y < 3 ? SHIELD : PASS));
        assertEquals(Exposure.SHELTERED, ShelterScan.evaluate(1, 10,
                y -> y == 2 || y == 5 || y == 9 ? SHIELD : PASS));
    }

    @Test
    void aboveTerrainIsExposedWithoutReadingBlocks() {
        assertEquals(Exposure.EXPOSED, ShelterScan.evaluate(100, 99, y -> {
            fail("There are no blocks above the head");
            return UNKNOWN;
        }));
    }

    @Test
    void incompleteTerrainCannotGrantRecoveryOrExposure() {
        assertEquals(Exposure.UNKNOWN, ShelterScan.evaluate(1, 10, y -> y == 5 ? UNKNOWN : PASS));
        assertEquals(Exposure.UNKNOWN, ShelterScan.evaluate(1, 10, y -> y < 3 ? SHIELD : UNKNOWN));
        assertEquals(Exposure.SHELTERED, ShelterScan.evaluate(1, 10, y -> y < 4 ? SHIELD : UNKNOWN));
    }

    @Test
    void scanStopsAfterTheThirdShieldAndSupportsNegativeHeights() {
        assertEquals(Exposure.SHELTERED, ShelterScan.evaluate(-5, 1000, y -> {
            assertTrue(y <= -3);
            return SHIELD;
        }));
    }

    @Test
    void tallAirColumnsReadOncePerSectionInsteadOfOncePerBlock() {
        int[] reads = {0};
        var airColumn = new ShelterScan.Column() {
            @Override
            public ShelterScan.Block sample(int y) { reads[0]++; return PASS; }
            @Override
            public int endOfRun(int y) { return y | 31; }
        };
        assertEquals(Exposure.EXPOSED, ShelterScan.evaluate(0, 4095, airColumn));
        assertEquals(128, reads[0]);
    }

    @Test
    void skippedMissingSectionsRemainUnknownUnlessKnownRoofIsSufficient() {
        var column = new ShelterScan.Column() {
            @Override
            public ShelterScan.Block sample(int y) { return y < 32 ? UNKNOWN : SHIELD; }
            @Override
            public int endOfRun(int y) { return y < 32 ? y | 31 : y; }
        };
        assertEquals(Exposure.UNKNOWN, ShelterScan.evaluate(0, 33, column));
        assertEquals(Exposure.SHELTERED, ShelterScan.evaluate(0, 34, column));
    }

    @Test
    void runBoundariesAreClippedWithoutOverflow() {
        var solidColumn = new ShelterScan.Column() {
            @Override
            public ShelterScan.Block sample(int y) { return SHIELD; }
            @Override
            public int endOfRun(int y) { return y | 31; }
        };
        assertEquals(Exposure.EXPOSED, ShelterScan.evaluate(Integer.MAX_VALUE - 1, Integer.MAX_VALUE, solidColumn));
        assertEquals(Exposure.SHELTERED, ShelterScan.evaluate(Integer.MAX_VALUE - 2, Integer.MAX_VALUE, solidColumn));
    }
}
