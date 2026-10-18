package io.github.nicolamigliore.radiation;

public enum Exposure {
    UNKNOWN(0), EXPOSED(1), SHELTERED(-1), NIGHT(-1), RECOVERING(-1), DISABLED(0), EXEMPT(0), DEAD(0);

    private final int doseDirection;

    Exposure(int doseDirection) {
        this.doseDirection = doseDirection;
    }

    public float advance(float dose, float seconds, RadiationSettings settings) {
        if (!Float.isFinite(seconds) || seconds < 0) {
            throw new IllegalArgumentException("Elapsed seconds must be finite and nonnegative");
        }
        float rate = switch (doseDirection) {
            case 1 -> settings.getExposureDosePerSecond();
            case -1 -> -settings.getRecoveryDosePerSecond();
            default -> 0f;
        };
        // Double intermediates keep finite rates/timesteps from overflowing before clamping.
        return RadLevelTable.clampDose((float) (RadLevelTable.clampDose(dose) + (double) rate * seconds));
    }
}
