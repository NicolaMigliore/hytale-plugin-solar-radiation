package io.github.nicolamigliore.radiation;

import java.util.function.IntFunction;

/** Three qualifying blocks anywhere above the head shield sunlight. */
public final class ShelterScan {
    public enum Block { SHIELD, PASS, UNKNOWN }

    private ShelterScan() {}

    public static Exposure evaluate(int firstY, int topY, IntFunction<Block> sample) {
        int shields = 0;
        boolean unknown = false;
        for (int y = firstY; y <= topY; y++) {
            switch (sample.apply(y)) {
                case SHIELD -> { if (++shields == 3) return Exposure.SHELTERED; }
                case UNKNOWN -> unknown = true;
                case PASS -> { }
            }
            if (y == Integer.MAX_VALUE) break;
        }
        return unknown ? Exposure.UNKNOWN : Exposure.EXPOSED;
    }
}
