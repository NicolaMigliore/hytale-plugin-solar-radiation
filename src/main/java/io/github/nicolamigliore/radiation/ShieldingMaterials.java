package io.github.nicolamigliore.radiation;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.protocol.Opacity;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import java.util.Set;
import static io.github.nicolamigliore.radiation.ShelterScan.Block.*;

public final class ShieldingMaterials {
    private ShieldingMaterials() {}

    public static ShelterScan.Block classify(BlockType block, RadiationSettings settings) {
        if (block == null || block.isUnknown()) return UNKNOWN;
        if (block.getMaterial() == BlockMaterial.Empty) return PASS;
        // Exact IDs allow exceptions to broad tag rules. Exclusions win at equal specificity.
        if (settings.isExplicitNonShield(block.getId())) return PASS;
        if (settings.isExplicitShield(block.getId())) return SHIELD;
        if (hasTag(block, settings.getNonShieldingTags())) return PASS;
        if (hasTag(block, settings.getShieldingTags())) return SHIELD;
        return block.getMaterial() == BlockMaterial.Solid && block.getOpacity() == Opacity.Solid
                && block.isCubeDrawType() && "Full".equals(block.getHitboxType())
                && !"Leaves".equals(block.getGroup()) ? SHIELD : PASS;
    }

    private static boolean hasTag(BlockType block, Set<String> tags) {
        if (block.getData() == null) return false;
        var indexes = block.getData().getExpandedTagIndexes();
        for (String tag : tags) {
            int index = AssetRegistry.getTagIndex(tag);
            if (index >= 0 && indexes.contains(index)) return true;
        }
        return false;
    }
}
