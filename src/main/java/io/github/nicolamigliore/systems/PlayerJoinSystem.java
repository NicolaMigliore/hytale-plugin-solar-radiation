package io.github.nicolamigliore.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.components.RadiationComponent;
import javax.annotation.Nonnull;

public class PlayerJoinSystem extends RefSystem<EntityStore> {
    @Override
    public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason,
                              @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {
        var radiation = store.getComponent(ref, RadiationComponent.getComponentType());
        if (radiation == null) {
            buffer.addComponent(ref, RadiationComponent.getComponentType(), new RadiationComponent());
        }
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason,
                               @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {}

    @Override
    public Query<EntityStore> getQuery() { return PlayerRef.getComponentType(); }
}
