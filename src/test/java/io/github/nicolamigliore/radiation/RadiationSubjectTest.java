package io.github.nicolamigliore.radiation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static io.github.nicolamigliore.radiation.RadiationSymptoms.Poison.*;
import static org.junit.jupiter.api.Assertions.*;

class RadiationSubjectTest {
    private final RadiationSettings settings = new RadiationSettings();

    @ParameterizedTest
    @CsvSource({"0,false,NONE", "9.999,false,NONE", "10,true,NONE", "119.999,true,NONE",
            "120,true,LOW", "179.999,true,LOW", "180,true,HIGH"})
    void playerSymptomsKeepExistingStages(float dose, boolean slowed, RadiationSymptoms.Poison poison) {
        assertEquals(new RadiationSymptoms(slowed, poison), RadiationSubject.PLAYER.symptoms(dose));
    }

    @Test
    void mobPoisonStartsBelowPlayerSymptomThresholdAndNeverAddsSlowOrHighPoison() {
        assertEquals(RadiationSymptoms.NONE, RadiationSubject.MOB.symptoms(0));
        for (float dose : new float[]{0.001f, 9, 10, 120, 180}) {
            assertEquals(new RadiationSymptoms(false, LOW), RadiationSubject.MOB.symptoms(dose));
        }
    }

    @Test
    void mobsRecoverWithoutSunlightOrPlayerDataAndStopAtZero() {
        assertEquals(9f, RadiationSubject.MOB.advance(10, Exposure.UNKNOWN, 1, settings));
        assertEquals(9f, RadiationSubject.MOB.advance(10, Exposure.EXPOSED, 1, settings));
        assertEquals(11f, RadiationSubject.PLAYER.advance(10, Exposure.EXPOSED, 1, settings));
        assertEquals(0f, RadiationSubject.MOB.advance(0.1f, Exposure.EXPOSED, 1, settings));
    }

    @Test
    void pausedStatesFreezeBothSubjectTypes() {
        for (RadiationSubject subject : RadiationSubject.values()) {
            for (Exposure paused : new Exposure[]{Exposure.DISABLED, Exposure.EXEMPT, Exposure.DEAD}) {
                assertEquals(100f, subject.advance(100, paused, 5, settings));
            }
        }
    }
}
