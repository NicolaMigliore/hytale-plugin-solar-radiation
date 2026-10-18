package io.github.nicolamigliore.radiation;

/** A single desired native effect set, shared by players and mobs. */
public record RadiationSymptoms(boolean slowed, Poison poison) {
    public enum Poison { NONE, LOW, HIGH }
    public static final RadiationSymptoms NONE = new RadiationSymptoms(false, Poison.NONE);
}
