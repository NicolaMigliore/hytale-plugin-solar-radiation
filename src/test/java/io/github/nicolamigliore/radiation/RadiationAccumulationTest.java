package io.github.nicolamigliore.radiation;

import io.github.nicolamigliore.components.RadiationComponent;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.github.nicolamigliore.radiation.RadiationSymptoms.Poison.*;
import static org.junit.jupiter.api.Assertions.*;

class RadiationAccumulationTest {
    private final RadiationSettings settings = settings(2.5f, 0.75f);

    @Test
    void ratesRoundTripAndOldConfigsKeepOneDosePerSecond() {
        var defaults = RadiationSettings.CODEC.decode(BsonDocument.parse("{\"ShelterDepth\":3}"));
        assertEquals(1f, defaults.getExposureDosePerSecond());
        assertEquals(1f, defaults.getRecoveryDosePerSecond());
        var restored = RadiationSettings.CODEC.decode(RadiationSettings.CODEC.encode(settings));
        assertEquals(2.5f, restored.getExposureDosePerSecond());
        assertEquals(0.75f, restored.getRecoveryDosePerSecond());
    }

    @Test
    void configuredRatesApplyPerSimulationSecondToPlayersAndMobs() {
        assertEquals(25f, RadiationSubject.PLAYER.advance(20, Exposure.EXPOSED, 2, settings));
        for (Exposure safe : new Exposure[]{Exposure.SHELTERED, Exposure.NIGHT, Exposure.RECOVERING}) {
            assertEquals(18.5f, RadiationSubject.PLAYER.advance(20, safe, 2, settings));
        }
        // Sunlight never changes the mob recovery policy.
        assertEquals(18.5f, RadiationSubject.MOB.advance(20, Exposure.EXPOSED, 2, settings));
        for (Exposure paused : new Exposure[]{Exposure.UNKNOWN, Exposure.DISABLED, Exposure.EXEMPT, Exposure.DEAD}) {
            assertEquals(20f, RadiationSubject.PLAYER.advance(20, paused, 2, settings));
        }
    }

    @Test
    void ratesCanBeDisabledIndependently() {
        assertEquals(20f, Exposure.EXPOSED.advance(20, 10, settings(0, 3)));
        assertEquals(0f, Exposure.NIGHT.advance(20, 10, settings(0, 3)));
        assertEquals(40f, Exposure.EXPOSED.advance(20, 10, settings(2, 0)));
        assertEquals(20f, Exposure.SHELTERED.advance(20, 10, settings(2, 0)));
        assertEquals(20f, RadiationSubject.MOB.advance(20, Exposure.EXPOSED, 10, settings(2, 0)));
    }

    @ParameterizedTest
    @ValueSource(floats = {0.025f, 0.125f, 0.5f, 2f})
    void differentTickLengthsGiveEquivalentDoseAndRecovery(float dt) {
        float dose = 20;
        int ticks = Math.round(8 / dt);
        for (int i = 0; i < ticks; i++) dose = Exposure.EXPOSED.advance(dose, dt, settings);
        assertEquals(40f, dose, 0.001f);
        for (int i = 0; i < ticks; i++) dose = Exposure.SHELTERED.advance(dose, dt, settings);
        assertEquals(34f, dose, 0.001f);
    }

    @ParameterizedTest
    @ValueSource(floats = {10, 30, 60, 90, 120, 180})
    void thresholdsAreCrossedInBothDirectionsWithoutRounding(float threshold) {
        var dose = new RadiationComponent(threshold - 0.25f);
        int previousStage = dose.getRadLevel();
        dose.setTotalRadiations(Exposure.EXPOSED.advance(dose.getTotalRadiations(), 0.125f, settings));
        assertEquals(previousStage + 1, dose.getRadLevel());
        dose.setTotalRadiations(Exposure.SHELTERED.advance(dose.getTotalRadiations(), 0.5f, settings));
        assertEquals(previousStage, dose.getRadLevel());
    }

    @Test
    void skippingStagesResolvesFinalSymptomsAndRecoveryClearsThem() {
        var dose = new RadiationComponent(0);
        var fast = settings(180, 60);
        dose.setTotalRadiations(Exposure.EXPOSED.advance(dose.getTotalRadiations(), 1, fast));
        assertEquals(6, dose.getRadLevel());
        assertEquals(new RadiationSymptoms(true, HIGH), RadiationSubject.PLAYER.symptoms(dose.getTotalRadiations()));
        dose.setTotalRadiations(Exposure.NIGHT.advance(dose.getTotalRadiations(), 1, fast));
        assertEquals(5, dose.getRadLevel());
        assertEquals(new RadiationSymptoms(true, LOW), RadiationSubject.PLAYER.symptoms(dose.getTotalRadiations()));
        dose.setTotalRadiations(Exposure.NIGHT.advance(dose.getTotalRadiations(), 1, fast));
        assertEquals(3, dose.getRadLevel());
        assertEquals(new RadiationSymptoms(true, NONE), RadiationSubject.PLAYER.symptoms(dose.getTotalRadiations()));
        dose.setTotalRadiations(Exposure.NIGHT.advance(dose.getTotalRadiations(), 1, fast));
        assertEquals(0, dose.getRadLevel());
        assertEquals(RadiationSymptoms.NONE, RadiationSubject.PLAYER.symptoms(dose.getTotalRadiations()));
        assertEquals(RadiationSymptoms.NONE, RadiationSubject.MOB.symptoms(dose.getTotalRadiations()));
    }

    @Test
    void hugeFiniteRatesAndTimestepsStayBoundedAndZeroElapsedTimeDoesNothing() {
        var huge = settings(Float.MAX_VALUE, Float.MAX_VALUE);
        assertEquals(180f, Exposure.EXPOSED.advance(20, Float.MAX_VALUE, huge));
        assertEquals(0f, Exposure.SHELTERED.advance(20, Float.MAX_VALUE, huge));
        assertEquals(20f, Exposure.EXPOSED.advance(20, 0, huge));
        assertEquals(20f, Exposure.NIGHT.advance(20, 0, huge));
    }

    @Test
    void negativeAndNonfiniteRatesOrElapsedTimesAreRejected() {
        for (String key : new String[]{"ExposureDosePerSecond", "RecoveryDosePerSecond"}) {
            for (double invalid : new double[]{-0.1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
                var config = new BsonDocument(key, new BsonDouble(invalid));
                assertThrows(RuntimeException.class, () -> RadiationSettings.CODEC.decode(config), key + "=" + invalid);
            }
        }
        for (float invalid : new float[]{-1, Float.NaN, Float.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> Exposure.EXPOSED.advance(20, invalid, settings));
        }
    }

    private static RadiationSettings settings(float exposure, float recovery) {
        return RadiationSettings.CODEC.decode(new BsonDocument()
                .append("ExposureDosePerSecond", new BsonDouble(exposure))
                .append("RecoveryDosePerSecond", new BsonDouble(recovery)));
    }
}
