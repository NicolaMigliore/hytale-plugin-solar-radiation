package io.github.nicolamigliore.radiation;

import io.github.nicolamigliore.components.RadiationRuntimeComponent;
import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExposureSettingsTest {
    @Test
    void oldConfigGetsSafeNewDefaultsAndKeepsDisabledWorlds() {
        var settings = RadiationSettings.CODEC.decode(BsonDocument.parse("{\"DisabledWorlds\":[\"instance\"]}"));
        assertEquals(3, settings.getShelterDepth());
        assertEquals(0.5f, settings.getExposureCheckInterval());
        assertTrue(settings.getNonShieldingTags().contains("Family=Leaves"));
        assertFalse(settings.isEnabled("instance"));
        assertTrue(settings.isEnabled("default"));
    }

    @Test
    void configuredDepthAndIntervalRoundTrip() {
        var settings = RadiationSettings.CODEC.decode(BsonDocument.parse(
                "{\"ShelterDepth\":2,\"ExposureCheckIntervalSeconds\":0.25,\"ShieldingTags\":[\"Type=Rock\"]}"));
        var decoded = RadiationSettings.CODEC.decode(RadiationSettings.CODEC.encode(settings));
        assertEquals(2, decoded.getShelterDepth());
        assertEquals(0.25f, decoded.getExposureCheckInterval());
        assertTrue(decoded.getShieldingTags().contains("Type=Rock"));
    }

    @Test
    void invalidDepthIntervalsAndBlankEntriesAreRejected() {
        for (String json : new String[]{"{\"ShelterDepth\":0}", "{\"ShelterDepth\":-1}",
                "{\"ExposureCheckIntervalSeconds\":0}", "{\"ExposureCheckIntervalSeconds\":-0.1}",
                "{\"ExposureCheckIntervalSeconds\":2}", "{\"ShieldingBlocks\":[\" \"]}"}) {
            assertThrows(RuntimeException.class, () -> RadiationSettings.CODEC.decode(BsonDocument.parse(json)), json);
        }
    }

    @Test
    void stableTerrainIsSampledOnACadenceAndTransitionsCanInvalidateIt() {
        var runtime = new RadiationRuntimeComponent();
        assertTrue(runtime.shouldCheckExposure(0, 0.5f));
        assertFalse(runtime.shouldCheckExposure(0.125f, 0.5f));
        assertFalse(runtime.shouldCheckExposure(0.125f, 0.5f));
        assertFalse(runtime.shouldCheckExposure(0.125f, 0.5f));
        assertTrue(runtime.shouldCheckExposure(0.125f, 0.5f));
        runtime.invalidateExposure();
        assertTrue(runtime.shouldCheckExposure(0, 0.5f));
    }
}
