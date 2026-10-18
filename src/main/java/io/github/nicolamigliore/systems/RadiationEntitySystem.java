package io.github.nicolamigliore.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.components.RadiationComponent;
import io.github.nicolamigliore.components.RadiationRuntimeComponent;
import javax.annotation.Nonnull;

/** Fresh temporary state on load or world transfer, for players and mobs alike. */
public class RadiationEntitySystem extends RefSystem<EntityStore> {
    @Override
    public Query<EntityStore> getQuery() { return RadiationComponent.getComponentType(); }

    @Override
    public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason,
                              @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {
        buffer.putComponent(ref, RadiationRuntimeComponent.getComponentType(), new RadiationRuntimeComponent());
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason,
                               @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {}
}
