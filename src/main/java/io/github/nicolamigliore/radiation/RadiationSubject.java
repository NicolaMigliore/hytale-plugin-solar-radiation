package io.github.nicolamigliore.radiation;

import static io.github.nicolamigliore.radiation.RadiationSymptoms.Poison.*;

/** Mobs opt in through their dose component, independently of player-only sunlight rules. */
public enum RadiationSubject {
    PLAYER, MOB;

    public float advance(float dose, Exposure exposure, float seconds) {
        var effectiveExposure = switch (exposure) {
            case DEAD, DISABLED, EXEMPT -> exposure;
            default -> this == MOB ? Exposure.RECOVERING : exposure;
        };
        return effectiveExposure.advance(dose, seconds);
    }

    public RadiationSymptoms symptoms(float dose) {
        if (this == MOB) {
            return dose > 0 ? new RadiationSymptoms(false, LOW) : RadiationSymptoms.NONE;
        }
        int stage = RadLevelTable.getRadLevelForRads(dose);
        return new RadiationSymptoms(stage >= 1, stage == 6 ? HIGH : stage == 5 ? LOW : NONE);
    }
}
