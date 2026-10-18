package io.github.nicolamigliore.radiation;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class RadiationSettings {
    public static final BuilderCodec<RadiationSettings> CODEC = BuilderCodec
            .builder(RadiationSettings.class, RadiationSettings::new)
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN),
                    (s, v) -> s.enabled = v, s -> s.enabled).add()
            .append(new KeyedCodec<>("DisabledWorlds", Codec.STRING_ARRAY),
                    (s, v) -> s.disabledWorlds = entries(v), s -> s.disabledWorlds.toArray(String[]::new)).add()
            .append(new KeyedCodec<>("ShelterDepth", Codec.INTEGER),
                    RadiationSettings::setShelterDepth, RadiationSettings::getShelterDepth).add()
            .append(new KeyedCodec<>("ExposureCheckIntervalSeconds", Codec.FLOAT),
                    RadiationSettings::setExposureCheckInterval, RadiationSettings::getExposureCheckInterval).add()
            .append(new KeyedCodec<>("ShieldingBlocks", Codec.STRING_ARRAY),
                    (s, v) -> s.shieldingBlocks = entries(v), s -> s.shieldingBlocks.toArray(String[]::new)).add()
            .append(new KeyedCodec<>("NonShieldingBlocks", Codec.STRING_ARRAY),
                    (s, v) -> s.nonShieldingBlocks = entries(v), s -> s.nonShieldingBlocks.toArray(String[]::new)).add()
            .append(new KeyedCodec<>("ShieldingTags", Codec.STRING_ARRAY),
                    (s, v) -> s.shieldingTags = entries(v), s -> s.shieldingTags.toArray(String[]::new)).add()
            .append(new KeyedCodec<>("NonShieldingTags", Codec.STRING_ARRAY),
                    (s, v) -> s.nonShieldingTags = entries(v), s -> s.nonShieldingTags.toArray(String[]::new)).add()
            .build();

    private boolean enabled = true;
    private Set<String> disabledWorlds = Set.of();
    private int shelterDepth = 3;
    private float exposureCheckInterval = 0.5f;
    private Set<String> shieldingBlocks = Set.of();
    private Set<String> nonShieldingBlocks = Set.of();
    private Set<String> shieldingTags = Set.of();
    private Set<String> nonShieldingTags = Set.of("Family=Leaves");

    public boolean isEnabled(String world) { return enabled && !disabledWorlds.contains(world); }
    public boolean isExplicitShield(String block) { return shieldingBlocks.contains(block); }
    public boolean isExplicitNonShield(String block) { return nonShieldingBlocks.contains(block); }
    public Set<String> getShieldingTags() { return shieldingTags; }
    public Set<String> getNonShieldingTags() { return nonShieldingTags; }
    public int getShelterDepth() { return shelterDepth; }
    public float getExposureCheckInterval() { return exposureCheckInterval; }

    private void setShelterDepth(int depth) {
        if (depth < 1) throw new IllegalArgumentException("ShelterDepth must be at least 1");
        shelterDepth = depth;
    }

    private void setExposureCheckInterval(float seconds) {
        if (!Float.isFinite(seconds) || seconds <= 0 || seconds > 1) {
            throw new IllegalArgumentException("ExposureCheckIntervalSeconds must be greater than 0 and at most 1");
        }
        exposureCheckInterval = seconds;
    }

    private static Set<String> entries(String[] values) {
        for (String value : values) {
            if (value == null || value.isBlank()) throw new IllegalArgumentException("Entries must not be blank");
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(values)));
    }
}
