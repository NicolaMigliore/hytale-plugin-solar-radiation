package io.github.nicolamigliore.radiation;

import org.junit.jupiter.api.Test;
import static io.github.nicolamigliore.radiation.ShelterScan.Block.*;
import static org.junit.jupiter.api.Assertions.*;

class ShelterScanTest {
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
}
