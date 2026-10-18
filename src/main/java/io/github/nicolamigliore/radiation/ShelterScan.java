package io.github.nicolamigliore.radiation;

/** Qualifying blocks may be separated; known air and missing sections can be skipped in runs. */
public final class ShelterScan {
    public enum Block { SHIELD, PASS, UNKNOWN }

    @FunctionalInterface
    public interface Column {
        Block sample(int y);

        /** Last block with the same classification as sample(y). */
        default int endOfRun(int y) { return y; }
    }

    private ShelterScan() {}

    public static Exposure evaluate(int firstY, int topY, Column column) {
        return evaluate(firstY, topY, 3, column);
    }

    public static Exposure evaluate(int firstY, int topY, int requiredShields, Column column) {
        if (requiredShields < 1) throw new IllegalArgumentException("Shelter depth must be positive");
        long shields = 0;
        boolean unknown = false;
        for (int y = firstY; y <= topY;) {
            Block block = column.sample(y);
            int lastY = Math.min(topY, Math.max(y, column.endOfRun(y)));
            switch (block) {
                case SHIELD -> {
                    shields += (long) lastY - y + 1;
                    if (shields >= requiredShields) return Exposure.SHELTERED;
                }
                case UNKNOWN -> unknown = true;
                case PASS -> { }
            }
            if (lastY == topY) break;
            y = lastY + 1;
        }
        return unknown ? Exposure.UNKNOWN : Exposure.EXPOSED;
    }
}
