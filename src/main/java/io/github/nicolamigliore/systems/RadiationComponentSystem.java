package io.github.nicolamigliore.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.components.RadiationComponent;
import io.github.nicolamigliore.components.RadiationRuntimeComponent;
import io.github.nicolamigliore.radiation.RadiationEffects;
import io.github.nicolamigliore.radiation.RadiationSymptoms;
import javax.annotation.Nonnull;

/** Supports opting an existing mob in/out by adding/removing its dose component. */
public class RadiationComponentSystem extends RefChangeSystem<EntityStore, RadiationComponent> {
    private final ComponentType<EntityStore, EffectControllerComponent> effectsType;

    public RadiationComponentSystem() { this(EffectControllerComponent.getComponentType()); }

    public RadiationComponentSystem(ComponentType<EntityStore, EffectControllerComponent> effectsType) {
        this.effectsType = effectsType;
    }

    @Override
    public ComponentType<EntityStore, RadiationComponent> componentType() { return RadiationComponent.getComponentType(); }

    @Override
    public Query<EntityStore> getQuery() { return Query.any(); }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull RadiationComponent dose,
                                 @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {
        buffer.putComponent(ref, RadiationRuntimeComponent.getComponentType(), new RadiationRuntimeComponent());
    }

    @Override
    public void onComponentSet(@Nonnull Ref<EntityStore> ref, @Nonnull RadiationComponent oldDose,
                               @Nonnull RadiationComponent dose, @Nonnull Store<EntityStore> store,
                               @Nonnull CommandBuffer<EntityStore> buffer) {
        buffer.ensureComponent(ref, RadiationRuntimeComponent.getComponentType());
    }

    @Override
    public void onComponentRemoved(@Nonnull Ref<EntityStore> ref, @Nonnull RadiationComponent dose,
                                   @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {
        buffer.tryRemoveComponent(ref, RadiationRuntimeComponent.getComponentType());
        var effects = buffer.getComponent(ref, effectsType);
        if (effects != null) RadiationEffects.update(ref, effects, RadiationSymptoms.NONE, buffer);
    }
}
