package io.github.nicolamigliore.radiation;

import io.github.nicolamigliore.components.RadiationComponent;
import java.util.concurrent.atomic.AtomicInteger;
import org.bson.BsonDocument;
import org.joml.Vector3d;
import com.hypixel.hytale.math.shape.Box;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class DecontaminationTest {
    @ParameterizedTest
    @ValueSource(floats = {0, 5, 10, 119, 140, 180})
    void treatmentClearsDoseAndSymptomsImmediatelyWithoutRemovingCapability(float amount) {
        var dose = new RadiationComponent(amount);
        var cleared = new AtomicInteger();
        assertTrue(DecontaminationInteraction.clearDose(dose, () -> {
            assertEquals(0, dose.getTotalRadiations());
            cleared.incrementAndGet();
        }));
        assertEquals(0, dose.getRadLevel());
        assertEquals(1, cleared.get());
        dose.setTotalRadiations(1); // No immunity: future radiation can accumulate.
        assertEquals(1, dose.getTotalRadiations());
    }

    @Test
    void ordinaryUntrackedMobsDoNotAcquireRadiationCapabilityFromTreatment() {
        assertFalse(DecontaminationInteraction.clearDose(null, () -> fail("Must not clear unrelated effects")));
    }

    @Test
    void radiusIncludesBoundaryAndRejectsDistantAndInvalidPositions() {
        var origin = new Vector3d(0.5, 1.5, 0.5);
        assertTrue(TreatmentGeometry.canReach(origin, new Vector3d(3.5, 1.5, .5), 3, (x,y,z) -> true));
        assertFalse(TreatmentGeometry.canReach(origin, new Vector3d(3.5001, 1.5, .5), 3, (x,y,z) -> true));
        assertFalse(TreatmentGeometry.canReach(origin, new Vector3d(Double.NaN, 1, 0), 3, (x,y,z) -> true));
        assertTrue(TreatmentGeometry.canReach(origin, origin, 3, (x,y,z) -> true));
    }

    @Test
    void wallsBlockBothDirectionsIncludingNegativeCoordinatesAndDiagonalCorners() {
        assertFalse(TreatmentGeometry.canReach(new Vector3d(.5, 1.5, .5),
                new Vector3d(2.5, 1.5, .5), 3, (x,y,z) -> x != 1));
        assertFalse(TreatmentGeometry.canReach(new Vector3d(.5, 1.5, .5),
                new Vector3d(-1.5, 1.5, .5), 3, (x,y,z) -> x != -1));
        assertFalse(TreatmentGeometry.canReach(new Vector3d(.5, .5, .5),
                new Vector3d(1.5, 1.5, .5), 3, (x,y,z) -> !(x == 1 && y == 0)));
        assertFalse(TreatmentGeometry.canReach(new Vector3d(.5, 1.5, .5),
                new Vector3d(2.5, 1.5, .5), 3, (x,y,z) -> false));
    }

    @Test
    void treatmentRadiusDefaultsForOldConfigsAndRoundTrips() {
        assertEquals(3, RadiationSettings.CODEC.decode(new BsonDocument()).getTreatmentRadius());
        var settings = RadiationSettings.CODEC.decode(BsonDocument.parse("{\"TreatmentRadius\":4.5}"));
        assertEquals(4.5, RadiationSettings.CODEC.decode(RadiationSettings.CODEC.encode(settings)).getTreatmentRadius());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.6, 1.0, 1.8})
    void groundSplashReachesBodiesEvenWhenFeetAreSlightlyInsideTheFloor(double height) {
        var impact = new Vector3d(.5, 1, .5);
        var rayOrigin = TreatmentGeometry.surfaceOrigin(impact, new Vector3d(0, 1, 0));
        var feet = new Vector3d(1.5, .9999999, .5);
        TreatmentGeometry.OpenVoxel air = (x,y,z) -> y >= 1;
        assertFalse(TreatmentGeometry.canReach(rayOrigin, feet, 3, air)); // Previous feet-only check.
        assertTrue(TreatmentGeometry.canTreat(impact, rayOrigin, feet,
                Box.horizontallyCentered(.8, height, .8), 3, air));
        assertTrue(TreatmentGeometry.canTreat(impact, rayOrigin, feet, null, 3, air));
    }

    @Test
    void splashAtThrowersFeetCanTreatTheThrowerWithoutIgnoringWallsOrRange() {
        var impact = new Vector3d(.5, 1, .5);
        var rayOrigin = TreatmentGeometry.surfaceOrigin(impact, new Vector3d(0, 1, 0));
        var body = Box.horizontallyCentered(.6, 1.8, .6);
        TreatmentGeometry.OpenVoxel air = (x,y,z) -> y >= 1 && x != 2;
        assertTrue(TreatmentGeometry.canTreat(impact, rayOrigin, new Vector3d(.5, .9999999, .5), body, 3, air));
        assertFalse(TreatmentGeometry.canTreat(impact, rayOrigin, new Vector3d(3, 1, .5), body, 3, air));
        assertFalse(TreatmentGeometry.canTreat(impact, rayOrigin, new Vector3d(.5, 1, 3.501), body, 3, air));
        // A body's center may be farther than radius: the position still defines the boundary.
        assertTrue(TreatmentGeometry.canTreat(impact, rayOrigin, new Vector3d(.5, 1, 3.5), body, 3, air));
    }

    @Test
    void wallSurfaceSplashStaysOnTheImpactSide() {
        var impact = new Vector3d(1, 1.5, .5);
        var rayOrigin = TreatmentGeometry.surfaceOrigin(impact, new Vector3d(-1, 0, 0));
        var body = Box.horizontallyCentered(.6, 1, .6);
        TreatmentGeometry.OpenVoxel air = (x,y,z) -> x != 1;
        assertTrue(TreatmentGeometry.canTreat(impact, rayOrigin, new Vector3d(.5, 1, .5), body, 3, air));
        assertFalse(TreatmentGeometry.canTreat(impact, rayOrigin, new Vector3d(2.5, 1, .5), body, 3, air));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "17", "NaN", "Infinity"})
    void invalidRadiusRejected(String number) {
        var document = new BsonDocument("TreatmentRadius", new org.bson.BsonDouble(Double.parseDouble(number)));
        assertThrows(Exception.class, () -> RadiationSettings.CODEC.decode(document));
    }
}
