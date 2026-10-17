package io.github.nicolamigliore.radiation;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import java.util.Arrays;

public class RadiationSettings {
    public static final BuilderCodec<RadiationSettings> CODEC = BuilderCodec
            .builder(RadiationSettings.class, RadiationSettings::new)
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN),
                    (s, v) -> s.enabled = v, s -> s.enabled).add()
            .append(new KeyedCodec<>("DisabledWorlds", Codec.STRING_ARRAY),
                    (s, v) -> s.disabledWorlds = v, s -> s.disabledWorlds).add()
            .append(new KeyedCodec<>("ShieldingBlocks", Codec.STRING_ARRAY),
                    (s, v) -> s.shieldingBlocks = v, s -> s.shieldingBlocks).add()
            .append(new KeyedCodec<>("NonShieldingBlocks", Codec.STRING_ARRAY),
                    (s, v) -> s.nonShieldingBlocks = v, s -> s.nonShieldingBlocks).add()
            .build();

    private boolean enabled = true;
    private String[] disabledWorlds = {};
    private String[] shieldingBlocks = {};
    private String[] nonShieldingBlocks = {};

    public boolean isEnabled(String world) {
        return enabled && !Arrays.asList(disabledWorlds).contains(world);
    }

    public boolean isExplicitShield(String block) {
        return Arrays.asList(shieldingBlocks).contains(block);
    }

    public boolean isExplicitNonShield(String block) {
        return Arrays.asList(nonShieldingBlocks).contains(block);
    }
}
