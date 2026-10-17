package io.github.nicolamigliore.radiation;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.RemovalBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class RadiationEffects {
    private static final String SLOW = "SolarRadiation_Slow";
    private static final String LOW = "SolarRadiation_Poison_Low";
    private static final String HIGH = "SolarRadiation_Poison_High";

    private RadiationEffects() {}

    public static void validateAssets() {
        for (String id : new String[]{SLOW, LOW, HIGH}) {
            if (EntityEffect.getAssetMap().getAsset(id) == null) {
                throw new IllegalStateException("Missing radiation effect asset: " + id);
            }
        }
    }

    public static void update(Ref<EntityStore> ref, EffectControllerComponent controller,
                              int stage, ComponentAccessor<EntityStore> accessor) {
        set(ref, controller, SLOW, stage >= 1, accessor);
        // Remove the outgoing tier before applying its replacement.
        if (stage == 6) {
            set(ref, controller, LOW, false, accessor);
            set(ref, controller, HIGH, true, accessor);
        } else {
            set(ref, controller, HIGH, false, accessor);
            set(ref, controller, LOW, stage == 5, accessor);
        }
    }

    private static void set(Ref<EntityStore> ref, EffectControllerComponent controller,
                            String id, boolean wanted, ComponentAccessor<EntityStore> accessor) {
        int index = EntityEffect.getAssetMap().getIndex(id);
        if (wanted && !controller.hasEffect(index)) {
            controller.addInfiniteEffect(ref, index, EntityEffect.getAssetMap().getAsset(id), accessor);
        } else if (!wanted && controller.hasEffect(index)) {
            controller.removeEffect(ref, index, RemovalBehavior.COMPLETE, accessor);
        }
    }
}
