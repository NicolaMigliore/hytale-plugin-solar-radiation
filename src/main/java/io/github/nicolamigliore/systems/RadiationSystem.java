package io.github.nicolamigliore.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkLightData;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.components.RadiationComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
        RadiationComponent radiationComponent = archetypeChunk.getComponent(index, radiationComponentType);

        assert radiationComponent != null;

        // Notify player they are radiated
        Player player = archetypeChunk.getComponent(index, Player.getComponentType());
//        player.sendMessage(Message.raw("("+player.getDisplayName()+") You are being targeted by radiation system! Current rad level: " + radiationComponent.getRadiationLevel()));


        // TODO: Apply damage after elapsed time (Example: poison from https://hytalemodding.dev/en/docs/guides/ecs/example-ecs-plugin)

        // TODO: Increase radiation level timer...
        boolean isRadiated = false;
        if(!isRadiated && radiationComponent.getDuration() > 0.0f){
            radiationComponent.incrementTimer(dt);
        }

        int currentRadLevel = radiationComponent.getRadiationLevel();
        if(currentRadLevel > 0 && (radiationComponent.getDuration() == 0.0f || radiationComponent.getTimer() > radiationComponent.getDuration())){
            radiationComponent.setRadiationLevel(currentRadLevel - 1);
            radiationComponent.setTimer(0.0f);
            player.sendMessage(Message.raw(player.getDisplayName()+" - Current rad level: " + radiationComponent.getRadiationLevel()));
        }

        // If radiaiton expired remove component
        // TODO: ...
        if(radiationComponent.getRadiationLevel() <= 0){
            Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
            commandBuffer.tryRemoveComponent(ref, RadiationComponent.getComponentType());

            player.sendMessage(Message.raw("("+player.getDisplayName()+") Radiation has been removed."));
        }
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

    /**
     * Evaluate block position exposure to sunlight.
     * @param world The world to check
     * @param position The coordinates to check
     */
    private int evaluateSunlightExposure(World world, Vector3i position){
        ChunkStore chunkStore = world.getChunkStore();
        int x = position.x;
        int y = position.y;
        int z = position.z;
        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        Ref<ChunkStore> chunkReference = chunkStore.getChunkReference(chunkIndex);

        if (chunkReference != null && chunkReference.isValid()){
            BlockChunk blockChunkComponent = chunkStore.getStore().getComponent(chunkReference, BlockChunk.getComponentType());

            assert blockChunkComponent != null;

            BlockSection section = blockChunkComponent.getSectionAtBlockY(y);
            short lightValue = section.getGlobalLight().getLightRaw(x, y, z);
            int skyLight = (int) ChunkLightData.getLightValue(lightValue, 3);

            return skyLight;
        }
        return 0;
    }

}
