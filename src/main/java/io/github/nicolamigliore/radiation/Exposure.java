package io.github.nicolamigliore.radiation;

public enum Exposure {
    UNKNOWN(0), EXPOSED(1), SHELTERED(-1), NIGHT(-1), RECOVERING(-1), DISABLED(0), EXEMPT(0), DEAD(0);

    private final int doseDirection;

    Exposure(int doseDirection) {
        this.doseDirection = doseDirection;
    }

    public float advance(float dose, float seconds) {
        if (!Float.isFinite(seconds) || seconds < 0) {
            throw new IllegalArgumentException("Elapsed seconds must be finite and nonnegative");
        }
        return RadLevelTable.clampDose(dose + doseDirection * seconds);
    }
}
