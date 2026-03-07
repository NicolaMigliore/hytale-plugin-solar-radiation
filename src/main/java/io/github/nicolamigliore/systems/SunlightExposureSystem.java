package io.github.nicolamigliore.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.components.RadiationComponent;
import io.github.nicolamigliore.events.ChangeRadiationExposureEvent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class SunlightExposureSystem extends EntityTickingSystem<EntityStore> {

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(RadiationComponent.getComponentType());
    }


    @Override
    public void tick(
            float dt,
            int index,
            @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl CommandBuffer<EntityStore> commandBuffer
    ) {

        Player player = archetypeChunk.getComponent(index, Player.getComponentType());
        Ref<EntityStore> playerRef = player.getReference();
        RadiationComponent radiationComponent = archetypeChunk.getComponent(index, RadiationComponent.getComponentType());

        assert radiationComponent != null;

        World world = player.getWorld();
        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());

        boolean exposedToSunlight = isExposedToSun(world, transform.getPosition().toVector3i());

        // Change radiation exposure
        boolean oldExposure = radiationComponent.isExposedToRadiation();
        if (oldExposure != exposedToSunlight){
            ChangeRadiationExposureEvent.dispatch(playerRef, exposedToSunlight);
        }

    }

    private boolean isExposedToSun(World world, Vector3i pos) {

        int x = pos.x;
        int y = pos.y + 1; // start above head
        int z = pos.z;

        // If below minimum build height → not exposed
        if (y < ChunkUtil.MIN_Y) return false;

        int localX = x & 31;
        int localZ = z & 31;

        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
        if (chunk == null) return false;

        BlockChunk blockChunk = chunk.getBlockChunk();
        if (blockChunk == null) return false;

        // Walk upward until world max height
        for (int checkY = y; checkY < ChunkUtil.HEIGHT; checkY++) {

            BlockSection section = blockChunk.getSectionAtBlockY(checkY);
            if (section == null) continue;

            int localY = checkY & 31;

            int blockId = section.get(localX, localY, localZ);

            BlockType block = BlockType.getAssetMap().getAsset(blockId);
            boolean isSolid = block.getMaterial() == BlockMaterial.Solid;
            if (isSolid) return false;
        }

        return true; // nothing above → exposed
    }
}
