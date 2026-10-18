package io.github.nicolamigliore.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.components.RadiationComponent;
import io.github.nicolamigliore.components.RadiationRuntimeComponent;
import io.github.nicolamigliore.radiation.RadiationEffects;
import javax.annotation.Nonnull;

/** Reset even when death/respawn happens between two radiation ticks. */
public class RadiationDeathSystem extends RefChangeSystem<EntityStore, DeathComponent> {
    @Override
    public ComponentType<EntityStore, DeathComponent> componentType() { return DeathComponent.getComponentType(); }
    @Override
    public Query<EntityStore> getQuery() {
        return RadiationComponent.getComponentType();
    }
    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent death,
                                 @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {
        reset(ref, buffer);
    }
    @Override
    public void onComponentRemoved(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent death,
                                   @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {
        reset(ref, buffer);
    }
    @Override
    public void onComponentSet(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent oldDeath,
                               @Nonnull DeathComponent death, @Nonnull Store<EntityStore> store,
                               @Nonnull CommandBuffer<EntityStore> buffer) {}

    private void reset(Ref<EntityStore> ref, CommandBuffer<EntityStore> buffer) {
        var radiation = buffer.getComponent(ref, RadiationComponent.getComponentType());
        if (radiation != null) {
            radiation.setTotalRadiations(0);
            var runtime = buffer.getComponent(ref, RadiationRuntimeComponent.getComponentType());
            if (runtime != null) runtime.invalidateExposure();
            var effects = buffer.getComponent(ref, EffectControllerComponent.getComponentType());
            if (effects != null) RadiationEffects.clear(ref, effects, buffer);
        }
    }
}
