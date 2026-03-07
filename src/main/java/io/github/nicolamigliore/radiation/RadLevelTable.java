package io.github.nicolamigliore.radiation;

public class RadLevelTable {
    private static final long[] RAD_LEVEL_THRESHOLDS = {
            0,
            100,
            250
    };

    public static final int MAX_RAD_LEVEL = RAD_LEVEL_THRESHOLDS.length;
    public static final int START_RAD_LEVEL = 1;

    private RadLevelTable() {
    }

    /**
     * Finds the highest radiation level whose threshold has been passed by the entity rads.
     *
     * @param totalRads
     * @return
     */
    public static int getRadLevelForRads(long totalRads) {
        if (totalRads < 0) return START_RAD_LEVEL;

        for (int radLevel = MAX_RAD_LEVEL; radLevel >= START_RAD_LEVEL; radLevel--) {
            if (totalRads >= RAD_LEVEL_THRESHOLDS[radLevel - 1]) {
                return radLevel;
            }
        }
        return START_RAD_LEVEL;
    }

    public static long getRadsForRadLevel(int radLevel) {
        if (radLevel < START_RAD_LEVEL) return 0L;
        if (radLevel > MAX_RAD_LEVEL) return RAD_LEVEL_THRESHOLDS[MAX_RAD_LEVEL - 1];
        return RAD_LEVEL_THRESHOLDS[radLevel - 1];
    }

    public static long getRadsInCurrentRadLevel(long totalRads) {
        var radLevel = getRadLevelForRads(totalRads);
        return totalRads - getRadsForRadLevel(radLevel);
    }

    public static long getRadsToNextRadLevel(long totalRads) {
        var radLevel = getRadLevelForRads(totalRads);
        if (radLevel >= MAX_RAD_LEVEL) return 0L;
        return RAD_LEVEL_THRESHOLDS[radLevel] - totalRads;
    }

    public static float getProgressToNextRadLevel(long totalRads) {
        var radLevel = getRadLevelForRads(totalRads);
        if (radLevel >= MAX_RAD_LEVEL) return 1.0f;

        var currentThreshold = RAD_LEVEL_THRESHOLDS[radLevel - 1];
        var nextThreshold = RAD_LEVEL_THRESHOLDS[radLevel];
        var radsInLevel = totalRads - currentThreshold;
        var radsNeeded = nextThreshold - currentThreshold;

        return radsNeeded == 0 ? 1.0f : (float) radsInLevel / radsNeeded;
    }
}
