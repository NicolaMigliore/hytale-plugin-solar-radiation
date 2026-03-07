package io.github.nicolamigliore.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkLightData;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.components.RadiationComponent;
import io.github.nicolamigliore.components.RadiationComponentOld;
import io.github.nicolamigliore.events.ChangeRadiationEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.temporal.ChronoField;

public class RadiationSystem extends EntityTickingSystem<EntityStore> {
    private final ComponentType<EntityStore, RadiationComponent> radiationComponentType;

    public RadiationSystem(ComponentType<EntityStore, RadiationComponent> radiaitonComponentType){
        this.radiationComponentType = radiaitonComponentType;
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
            ){

        Player player = archetypeChunk.getComponent(index, Player.getComponentType());
        Ref<EntityStore> playerRef = player.getReference();
        RadiationComponent radiationComponent = archetypeChunk.getComponent(index, RadiationComponent.getComponentType());

        assert radiationComponent != null;

        float amount = 0.f;
        if (radiationComponent.isExposedToRadiation()){
            amount = dt * 1; // 1 radiation per second
        }else{
            amount = dt * -1;
        }
        ChangeRadiationEvent.dispatch(playerRef, amount);
    }

    // TODO: Understand what is happening here...
    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup(){
        return DamageModule.get().getGatherDamageGroup();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(this.radiationComponentType);
    }

}
