package io.github.nicolamigliore.radiation;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
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
        validateAssets(EntityEffect.getAssetMap());
    }

    static void validateAssets(IndexedLookupTableAssetMap<String, EntityEffect> assets) {
        for (String id : new String[]{SLOW, LOW, HIGH}) {
            var effect = assets.getAsset(id);
            if (effect == null) {
                throw new IllegalStateException("Missing radiation effect asset: " + id);
            }
            if (!effect.isInfinite() || effect.getRemovalBehavior() != RemovalBehavior.COMPLETE) {
                throw new IllegalStateException("Radiation effect must be infinite with complete removal: " + id);
            }
            if (!id.equals(SLOW) && (effect.getDamageCalculator() == null
                    || !Float.isFinite(effect.getDamageCalculatorCooldown())
                    || effect.getDamageCalculatorCooldown() <= 0)) {
                throw new IllegalStateException("Radiation poison must define damage and a positive cooldown: " + id);
            }
        }
    }

    public static void clear(Ref<EntityStore> ref, EffectControllerComponent controller,
                             ComponentAccessor<EntityStore> accessor) {
        if (!controller.getActiveEffects().isEmpty()) update(ref, controller, RadiationSymptoms.NONE, accessor);
    }

    public static void update(Ref<EntityStore> ref, EffectControllerComponent controller,
                              RadiationSymptoms symptoms, ComponentAccessor<EntityStore> accessor) {
        update(ref, controller, symptoms, accessor, EntityEffect.getAssetMap());
    }

    static void update(Ref<EntityStore> ref, EffectControllerComponent controller,
                       RadiationSymptoms symptoms, ComponentAccessor<EntityStore> accessor,
                       IndexedLookupTableAssetMap<String, EntityEffect> assets) {
        set(ref, controller, SLOW, symptoms.slowed(), accessor, assets);
        // Remove the outgoing tier before applying its replacement.
        if (symptoms.poison() == RadiationSymptoms.Poison.HIGH) {
            set(ref, controller, LOW, false, accessor, assets);
            set(ref, controller, HIGH, true, accessor, assets);
        } else {
            set(ref, controller, HIGH, false, accessor, assets);
            set(ref, controller, LOW, symptoms.poison() == RadiationSymptoms.Poison.LOW, accessor, assets);
        }
    }

    private static void set(Ref<EntityStore> ref, EffectControllerComponent controller,
                            String id, boolean wanted, ComponentAccessor<EntityStore> accessor,
                            IndexedLookupTableAssetMap<String, EntityEffect> assets) {
        int index = assets.getIndex(id);
        if (wanted && !controller.hasEffect(index)) {
            controller.addInfiniteEffect(ref, index, assets.getAsset(id), accessor);
        } else if (!wanted && controller.hasEffect(index)) {
            controller.removeEffect(ref, index, RemovalBehavior.COMPLETE, accessor);
        }
    }
}
