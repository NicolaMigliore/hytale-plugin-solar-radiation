package io.github.nicolamigliore.radiation;

public class RadLevelTable {
    private static final float[] RAD_LEVEL_THRESHOLDS = {0, 10, 30, 60, 90, 120, 180};

    public static final int MAX_RAD_LEVEL = RAD_LEVEL_THRESHOLDS.length - 1;
    public static final int START_RAD_LEVEL = 0;
    public static final float MAX_DOSE = 180f;

    public static float clampDose(float dose) {
        if (Float.isNaN(dose)) return 0;
        return Math.clamp(dose, 0f, MAX_DOSE);
    }

    private RadLevelTable() {
    }

    /** Highest stage whose threshold is reached, without rounding fractional dose. */
    public static int getRadLevelForRads(float totalRads) {
        if (totalRads < 0) return START_RAD_LEVEL;

        for (int radLevel = MAX_RAD_LEVEL; radLevel >= START_RAD_LEVEL; radLevel--) {
            if (totalRads >= RAD_LEVEL_THRESHOLDS[radLevel]) {
                return radLevel;
            }
        }
        return START_RAD_LEVEL;
    }

    public static float getRadsForRadLevel(int radLevel) {
        return RAD_LEVEL_THRESHOLDS[Math.clamp(radLevel, 0, MAX_RAD_LEVEL)];
    }

    public static float getRadsInCurrentRadLevel(float totalRads) {
        var radLevel = getRadLevelForRads(totalRads);
        return totalRads - getRadsForRadLevel(radLevel);
    }

    public static float getRadsToNextRadLevel(float totalRads) {
        var radLevel = getRadLevelForRads(totalRads);
        if (radLevel >= MAX_RAD_LEVEL) return 0f;
        return RAD_LEVEL_THRESHOLDS[radLevel + 1] - totalRads;
    }

    public static float getProgressToNextRadLevel(float totalRads) {
        var radLevel = getRadLevelForRads(totalRads);
        if (radLevel >= MAX_RAD_LEVEL) return 1.0f;

        var currentThreshold = RAD_LEVEL_THRESHOLDS[radLevel];
        var nextThreshold = RAD_LEVEL_THRESHOLDS[radLevel + 1];
        var radsInLevel = totalRads - currentThreshold;
        var radsNeeded = nextThreshold - currentThreshold;

        return radsNeeded == 0 ? 1.0f : Math.clamp(radsInLevel / radsNeeded, 0f, 1f);
    }
}
