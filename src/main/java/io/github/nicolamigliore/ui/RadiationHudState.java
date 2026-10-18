package io.github.nicolamigliore.ui;

import io.github.nicolamigliore.radiation.Exposure;
import io.github.nicolamigliore.radiation.RadLevelTable;

/** Only visible values participate in change detection. Stage uses the unrounded dose. */
public record RadiationHudState(int fillPixels, int stage, Exposure exposure) {
    // Matches the full-size native health bar; the fill uses the entire 318x12 track.
    public static final int BAR_WIDTH = 318;

    public static RadiationHudState of(float dose, Exposure exposure) {
        float bounded = RadLevelTable.clampDose(dose);
        return new RadiationHudState(Math.round(BAR_WIDTH * bounded / RadLevelTable.MAX_DOSE),
                RadLevelTable.getRadLevelForRads(bounded), exposure);
    }

    public float barValue() { return fillPixels / (float) BAR_WIDTH; }

    public String stageText() {
        String name = switch (stage) {
            case 1 -> "Irradiated";
            case 2 -> "Elevated";
            case 3 -> "High";
            case 4 -> "Severe";
            case 5 -> "Poisoned";
            case 6 -> "Critical";
            default -> "Healthy";
        };
        return "Stage " + stage + " - " + name;
    }

    public String exposureText() {
        return switch (exposure) {
            case EXPOSED -> "Exposure: Direct sunlight";
            case SHELTERED -> "Exposure: Sheltered";
            case NIGHT -> "Exposure: Night";
            case RECOVERING -> "Exposure: Recovering";
            case DISABLED -> "Exposure: Disabled in this world";
            case EXEMPT -> "Exposure: Exempt game mode";
            case DEAD -> "Exposure: Dead";
            case UNKNOWN -> "Exposure: Checking terrain...";
        };
    }
}
