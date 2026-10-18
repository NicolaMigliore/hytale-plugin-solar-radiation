package io.github.nicolamigliore.radiation;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.protocol.DrawType;
import com.hypixel.hytale.protocol.Opacity;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static io.github.nicolamigliore.radiation.ShelterScan.Block.*;
import static org.junit.jupiter.api.Assertions.*;

class ShieldingMaterialsTest {
    @Test
    void opaqueFullCubesShieldButGlassLeavesPartialBlocksAndEmptyMaterialDoNot() {
        var settings = new RadiationSettings();
        assertEquals(SHIELD, ShieldingMaterials.classify(new MaterialBlock("Stone"), settings));
        assertEquals(PASS, ShieldingMaterials.classify(new MaterialBlock("Glass").opacity(Opacity.Transparent), settings));
        assertEquals(PASS, ShieldingMaterials.classify(new MaterialBlock("Leaves").group("Leaves"), settings));
        assertEquals(PASS, ShieldingMaterials.classify(new MaterialBlock("Slab").hitbox("Slab"), settings));
        assertEquals(PASS, ShieldingMaterials.classify(new MaterialBlock("Fence").model(), settings));
        assertEquals(PASS, ShieldingMaterials.classify(new MaterialBlock("Air").empty(), settings));
        assertEquals(UNKNOWN, ShieldingMaterials.classify(null, settings));
    }

    @Test
    void explicitIDsAllowShieldingGlassAndExclusionsWin() {
        var glass = new MaterialBlock("Glass").opacity(Opacity.Transparent);
        var allowed = config("{\"ShieldingBlocks\":[\"Glass\"]}");
        assertEquals(SHIELD, ShieldingMaterials.classify(glass, allowed));
        var denied = config("{\"ShieldingBlocks\":[\"Glass\"],\"NonShieldingBlocks\":[\"Glass\"]}");
        assertEquals(PASS, ShieldingMaterials.classify(glass, denied));
    }

    @Test
    void nativeTagsMatchBothKeysAndValues() {
        var glass = new MaterialBlock("ShieldGlass").opacity(Opacity.Transparent)
                .tags(Map.of("SolarRadiation", new String[]{"Shielding"}));
        assertEquals(SHIELD, ShieldingMaterials.classify(glass, config(
                "{\"ShieldingTags\":[\"SolarRadiation=Shielding\"]}")));
        assertEquals(SHIELD, ShieldingMaterials.classify(glass, config(
                "{\"ShieldingTags\":[\"SolarRadiation\"]}")));
        assertEquals(PASS, ShieldingMaterials.classify(glass, config(
                "{\"ShieldingTags\":[\"SolarRadiation=NonShielding\"]}")));
    }

    @Test
    void exactIDsOverrideBroadTagsAndTagExclusionsWinTies() {
        var block = new MaterialBlock("Stone").tags(Map.of("Type", new String[]{"Rock"}));
        assertEquals(PASS, ShieldingMaterials.classify(block, config(
                "{\"NonShieldingTags\":[\"Type=Rock\"]}")));
        assertEquals(SHIELD, ShieldingMaterials.classify(block, config(
                "{\"NonShieldingTags\":[\"Type=Rock\"],\"ShieldingBlocks\":[\"Stone\"]}")));
        assertEquals(PASS, ShieldingMaterials.classify(block, config(
                "{\"ShieldingTags\":[\"Type=Rock\"],\"NonShieldingTags\":[\"Type=Rock\"]}")));
        assertEquals(PASS, ShieldingMaterials.classify(block, config(
                "{\"ShieldingTags\":[\"Type=Rock\"],\"NonShieldingBlocks\":[\"Stone\"]}")));
    }

    @Test
    void leafFamilyIsExcludedEvenWhenItsModelLooksLikeAnOpaqueCube() {
        var leaves = new MaterialBlock("CustomLeaves").tags(Map.of("Family", new String[]{"Leaves"}));
        assertEquals(PASS, ShieldingMaterials.classify(leaves, new RadiationSettings()));
    }

    private static RadiationSettings config(String json) {
        return RadiationSettings.CODEC.decode(BsonDocument.parse(json));
    }

    /** Real Hytale BlockType and tag metadata, populated without loading the full asset pack. */
    private static class MaterialBlock extends BlockType {
        MaterialBlock(String id) {
            super(id);
            material = BlockMaterial.Solid;
            drawType = DrawType.Cube;
            opacity = Opacity.Solid;
            hitboxType = "Full";
        }
        MaterialBlock opacity(Opacity value) { opacity = value; return this; }
        MaterialBlock group(String value) { group = value; return this; }
        MaterialBlock hitbox(String value) { hitboxType = value; return this; }
        MaterialBlock model() { drawType = DrawType.Model; return this; }
        MaterialBlock empty() { material = BlockMaterial.Empty; return this; }
        MaterialBlock tags(Map<String, String[]> tags) {
            data = new AssetExtraInfo.Data(BlockType.class, id, null);
            data.putTags(tags);
            return this;
        }
    }
}
