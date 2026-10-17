package io.github.nicolamigliore.radiation;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.protocol.Opacity;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.heightmap.HeightmapColumn;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public final class SolarExposure {
    private SolarExposure() {}

    public static Exposure sample(World world, int x, int headY, int z, RadiationSettings settings) {
        var chunks = world.getChunkStore();
        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        var heightmap = chunks.getChunkComponent(chunkIndex, HeightmapColumn.getComponentType());
        if (heightmap == null) return Exposure.UNKNOWN;
        int topY = Math.max(heightmap.getHeight(x & 31, z & 31),
                heightmap.getCollisionHeight(x & 31, z & 31));
        return ShelterScan.evaluate(headY, topY, new ColumnSampler(chunks, x, z, settings)::sample);
    }

    // Reuse each section lookup for its 32 blocks, without loading absent terrain.
    private static final class ColumnSampler {
        private final ChunkStore chunks;
        private final int x, z;
        private final RadiationSettings settings;
        private int sectionY = Integer.MIN_VALUE;
        private BlockSection section;

        private ColumnSampler(ChunkStore chunks, int x, int z, RadiationSettings settings) {
            this.chunks = chunks;
            this.x = x;
            this.z = z;
            this.settings = settings;
        }

        private ShelterScan.Block sample(int y) {
            if ((y >> 5) != sectionY) {
                sectionY = y >> 5;
                var ref = chunks.getChunkSectionReferenceAtBlock(x, y, z);
                section = ref == null || !ref.isValid() ? null
                        : chunks.getStore().getComponent(ref, BlockSection.getComponentType());
            }
            if (section == null) return ShelterScan.Block.UNKNOWN;
            int blockId = section.get(x & 31, y & 31, z & 31);
            if (blockId == 0) return ShelterScan.Block.PASS;
            var block = BlockType.getAssetMap().getAsset(blockId);
            if (block == null || block.isUnknown()) return ShelterScan.Block.UNKNOWN;
            if (settings.isExplicitNonShield(block.getId())) return ShelterScan.Block.PASS;
            if (settings.isExplicitShield(block.getId())) return ShelterScan.Block.SHIELD;
            boolean shields = block.getMaterial() == BlockMaterial.Solid
                    && block.getOpacity() == Opacity.Solid
                    && block.isCubeDrawType() && "Full".equals(block.getHitboxType())
                    && !"Leaves".equals(block.getGroup());
            return shields ? ShelterScan.Block.SHIELD : ShelterScan.Block.PASS;
        }
    }
}
