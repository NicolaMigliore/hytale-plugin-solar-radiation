package io.github.nicolamigliore.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.components.RadiationComponent;
import io.github.nicolamigliore.components.RadiationRuntimeComponent;
import io.github.nicolamigliore.radiation.Exposure;
import io.github.nicolamigliore.ui.RadiationHud;
import java.util.Set;
import javax.annotation.Nonnull;

/** Player-only presentation runs on the world thread after dose/exposure resolution. */
public class RadiationHudSystem extends EntityTickingSystem<EntityStore> {
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType(), PlayerRef.getComponentType());
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(new SystemDependency<>(Order.AFTER, RadiationSystem.class));
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {
        var ref = chunk.getReferenceTo(index);
        var player = chunk.getComponent(index, Player.getComponentType());
        var playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        var dose = buffer.getComponent(ref, RadiationComponent.getComponentType());
        var runtime = buffer.getComponent(ref, RadiationRuntimeComponent.getComponentType());
        RadiationHud.sync(player.getHudManager(), playerRef, !player.isWaitingForClientReady(),
                dt, dose, runtime == null ? Exposure.UNKNOWN : runtime.getExposure());
    }
}
