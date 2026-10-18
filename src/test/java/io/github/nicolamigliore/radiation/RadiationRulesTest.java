package io.github.nicolamigliore.radiation;

import io.github.nicolamigliore.components.RadiationComponent;
import io.github.nicolamigliore.components.RadiationRuntimeComponent;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class RadiationRulesTest {
    private final RadiationSettings settings = new RadiationSettings();

    @ParameterizedTest
    @CsvSource({"0,0", "9.999,0", "10,1", "29.999,1", "30,2", "59.999,2", "60,3",
            "89.999,3", "90,4", "119.999,4", "120,5", "179.999,5", "180,6"})
    void stagesUseFractionalDose(float dose, int stage) {
        assertEquals(stage, new RadiationComponent(dose).getRadLevel());
    }

    @Test
    void migrationClampsLegacyDoseAndDiscardsStaleExposure() {
        var legacy = BsonDocument.parse("{\"TotalRadiations\": 300.0, \"ExposedToRadiation\": true}");
        var radiation = RadiationComponent.CODEC.decode(legacy);
        assertEquals(180f, radiation.getTotalRadiations());
        var encoded = RadiationComponent.CODEC.encode(radiation).asDocument();
        assertFalse(encoded.containsKey("ExposedToRadiation"));
        assertEquals(180f, RadiationComponent.CODEC.decode(encoded).getTotalRadiations());
    }

    @Test
    void doseAndStageProgressAreBounded() {
        assertEquals(0f, new RadiationComponent(-5).getTotalRadiations());
        assertEquals(0f, new RadiationComponent(Float.NaN).getTotalRadiations());
        assertEquals(180f, new RadiationComponent(Float.POSITIVE_INFINITY).getTotalRadiations());
        assertEquals(0.5f, new RadiationComponent(20).getProgress());
        assertEquals(1f, new RadiationComponent(180).getProgress());
        assertEquals(0f, new RadiationComponent(180).getRadiationsToNextLevel());
    }

    @Test
    void exposureAndRecoveryUseElapsedSecondsAndClampAtLimits() {
        float fine = 0;
        for (int i = 0; i < 100; i++) fine = Exposure.EXPOSED.advance(fine, 0.1f, settings);
        assertEquals(Exposure.EXPOSED.advance(0, 10, settings), fine, 0.001f);
        assertEquals(180f, Exposure.EXPOSED.advance(179, 10, settings));
        assertEquals(0f, Exposure.SHELTERED.advance(5, 10, settings));
        assertEquals(90f, Exposure.NIGHT.advance(100, 10, settings));
        for (Exposure paused : new Exposure[]{Exposure.UNKNOWN, Exposure.EXEMPT, Exposure.DISABLED, Exposure.DEAD}) {
            assertEquals(100f, paused.advance(100, 10, settings));
        }
        assertThrows(IllegalArgumentException.class, () -> Exposure.EXPOSED.advance(0, Float.NaN, settings));
    }

    @Test
    void doseAndRuntimeCloneIndependently() {
        var original = new RadiationComponent(89.5f);
        var runtime = new RadiationRuntimeComponent();
        runtime.setExposure(Exposure.EXPOSED);
        assertTrue(runtime.shouldCheckExposure(0));
        var transferred = original.clone();
        assertEquals(89.5f, transferred.getTotalRadiations());
        transferred.setTotalRadiations(5);
        assertEquals(89.5f, original.getTotalRadiations());
        var transferredRuntime = runtime.clone();
        assertEquals(Exposure.UNKNOWN, transferredRuntime.getExposure());
        assertTrue(transferredRuntime.shouldCheckExposure(0));
        assertNull(runtime.cloneSerializable());
    }

    @Test
    void nativeWorldClockDistinguishesNoonAndMidnight() {
        var time = new WorldTimeResource();
        time.setGameTime0(Instant.parse("0001-01-01T12:00:00Z"));
        assertTrue(time.isScaledDayTimeWithinRange(0.25, 0.75));
        time.setGameTime0(Instant.parse("0001-01-01T00:00:00Z"));
        assertFalse(time.isScaledDayTimeWithinRange(0.25, 0.75));
    }
}
