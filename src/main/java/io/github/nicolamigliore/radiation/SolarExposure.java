package io.github.nicolamigliore.radiation;

import com.hypixel.hytale.math.util.ChunkUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
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
        if (headY > topY) return Exposure.EXPOSED;
        return ShelterScan.evaluate(headY, topY, settings.getShelterDepth(),
                new ColumnSampler(chunks, x, z, settings));
    }

    // Reuse each section lookup for its 32 blocks, without loading absent terrain.
    private static final class ColumnSampler implements ShelterScan.Column {
        private final ChunkStore chunks;
        private final int x, z;
        private final RadiationSettings settings;
        private int sectionY = Integer.MIN_VALUE;
        private BlockSection section;
        private boolean airSection;
        private final Int2ObjectOpenHashMap<ShelterScan.Block> materials = new Int2ObjectOpenHashMap<>();

        private ColumnSampler(ChunkStore chunks, int x, int z, RadiationSettings settings) {
            this.chunks = chunks;
            this.x = x;
            this.z = z;
            this.settings = settings;
        }

        @Override
        public ShelterScan.Block sample(int y) {
            if ((y >> 5) != sectionY) {
                sectionY = y >> 5;
                var ref = chunks.getChunkSectionReferenceAtBlock(x, y, z);
                section = ref == null || !ref.isValid() ? null
                        : chunks.getStore().getComponent(ref, BlockSection.getComponentType());
                airSection = section != null && section.isSolidAir();
            }
            if (section == null) return ShelterScan.Block.UNKNOWN;
            if (airSection) return ShelterScan.Block.PASS;
            int blockId = section.get(x & 31, y & 31, z & 31);
            if (blockId == 0) return ShelterScan.Block.PASS;
            var result = materials.get(blockId);
            if (result == null) {
                result = ShieldingMaterials.classify(BlockType.getAssetMap().getAsset(blockId), settings);
                materials.put(blockId, result);
            }
            return result;
        }

        @Override
        public int endOfRun(int y) {
            return section == null || airSection ? y | 31 : y;
        }
    }
}
