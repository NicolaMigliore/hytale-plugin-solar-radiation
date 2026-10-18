package io.github.nicolamigliore.radiation;

import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.RemovalBehavior;
import com.hypixel.hytale.server.core.entity.effect.ActiveEntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.livingentity.LivingEntityEffectSystem;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.combat.DamageCalculator;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.systems.RadiationSystem;
import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Tests our calls into the native effect controller; native damage/defenses need a live world. */
class RadiationEffectsTest {
    private static final String SLOW = "SolarRadiation_Slow";
    private static final String LOW = "SolarRadiation_Poison_Low";
    private static final String HIGH = "SolarRadiation_Poison_High";
    private final TestAssets assets = new TestAssets();
    private final RecordingController controller = new RecordingController(assets);

    @Test
    void escalationAndRecoveryReplacePoisonAndClearOnlyRadiationEffects() {
        update(RadiationSubject.PLAYER, 10);
        assertActive(SLOW);
        update(RadiationSubject.PLAYER, 120);
        assertActive(SLOW, LOW);
        controller.operations.clear();
        update(RadiationSubject.PLAYER, 180);
        assertActive(SLOW, HIGH);
        assertEquals(List.of("remove:" + LOW, "add:" + HIGH), controller.operations);

        controller.operations.clear();
        update(RadiationSubject.PLAYER, 179.999f);
        assertActive(SLOW, LOW);
        assertEquals(List.of("remove:" + HIGH, "add:" + LOW), controller.operations);
        update(RadiationSubject.PLAYER, 119.999f);
        assertActive(SLOW);
        update(RadiationSubject.PLAYER, 9.999f);
        assertActive();
    }

    @Test
    void repeatedUpdatesPreserveTheNativeEffectInstanceAndItsDamageTimer() {
        update(RadiationSubject.PLAYER, 120);
        var poison = controller.getActiveEffects().get(assets.getIndex(LOW));
        controller.operations.clear();
        for (int i = 0; i < 200; i++) update(RadiationSubject.PLAYER, 120 + i * 0.1f);
        assertSame(poison, controller.getActiveEffects().get(assets.getIndex(LOW)));
        assertTrue(controller.operations.isEmpty(), "Stable symptoms must not refresh poison each tick");
    }

    @Test
    void allSlowOnlyStagesKeepTheSameEffect() {
        update(RadiationSubject.PLAYER, 10);
        var slow = controller.getActiveEffects().get(assets.getIndex(SLOW));
        controller.operations.clear();
        for (float dose : new float[]{30, 60, 90, 60, 30, 10}) update(RadiationSubject.PLAYER, dose);
        assertSame(slow, controller.getActiveEffects().get(assets.getIndex(SLOW)));
        assertTrue(controller.operations.isEmpty());
    }

    @Test
    void clearingRadiationPreservesUnrelatedPoisonAndSlow() {
        seed("Slow");
        seed("Poison_T1");
        update(RadiationSubject.PLAYER, 180);
        reconcile(RadiationSymptoms.NONE);
        assertActive("Slow", "Poison_T1");
        controller.operations.clear();
        reconcile(RadiationSymptoms.NONE);
        assertTrue(controller.operations.isEmpty());
    }

    @Test
    void stageJumpsApplyOnlyTheFinalTierAndZeroRemovesIt() {
        update(RadiationSubject.PLAYER, 180);
        assertActive(SLOW, HIGH);
        assertFalse(controller.operations.contains("add:" + LOW));
        update(RadiationSubject.PLAYER, 0);
        assertActive();
    }

    @Test
    void mobsKeepLowPoisonWithoutPlayerEffectsUntilCompletelyRecovered() {
        for (float dose : new float[]{180, 120, 10, 0.001f}) {
            update(RadiationSubject.MOB, dose);
            assertActive(LOW);
        }
        update(RadiationSubject.MOB, 0);
        assertActive();
    }

    @Test
    void missingOrRejectedEffectsAreRetriedWithoutResettingExistingEffects() {
        controller.rejectApplications = true;
        update(RadiationSubject.PLAYER, 120);
        assertActive();
        controller.rejectApplications = false;
        update(RadiationSubject.PLAYER, 120);
        assertActive(SLOW, LOW);
        controller.getActiveEffects().remove(assets.getIndex(LOW));
        controller.operations.clear();
        update(RadiationSubject.PLAYER, 120);
        assertActive(SLOW, LOW);
        assertEquals(List.of("add:" + LOW), controller.operations);
    }

    @Test
    void radiationChangesPrecedeNativeEffectDamage() {
        var dependency = new RadiationSystem(new RadiationSettings()).getDependencies().iterator().next();
        var order = assertInstanceOf(SystemDependency.class, dependency);
        assertEquals(Order.BEFORE, order.getOrder());
        assertEquals(LivingEntityEffectSystem.class, order.getSystemClass());
    }

    @Test
    void startupRejectsMissingLingeringOrNonDamagingPoison() {
        assertDoesNotThrow(() -> RadiationEffects.validateAssets(assets));
        var poison = (TestEffect) assets.getAsset(LOW);
        poison.infinite(false);
        assertThrows(IllegalStateException.class, () -> RadiationEffects.validateAssets(assets));
        poison.infinite(true);
        poison.removal(RemovalBehavior.DURATION);
        assertThrows(IllegalStateException.class, () -> RadiationEffects.validateAssets(assets));
        poison.removal(RemovalBehavior.COMPLETE);
        poison.cooldown(0);
        assertThrows(IllegalStateException.class, () -> RadiationEffects.validateAssets(assets));
        poison.cooldown(5);
        poison.noDamage();
        assertThrows(IllegalStateException.class, () -> RadiationEffects.validateAssets(assets));
        assets.effects.remove(poison);
        assertThrows(IllegalStateException.class, () -> RadiationEffects.validateAssets(assets));
    }

    @ParameterizedTest
    @CsvSource({"SolarRadiation_Slow,Slow", "SolarRadiation_Poison_Low,Poison_T1",
            "SolarRadiation_Poison_High,Poison_T2"})
    void packagedAssetsInheritNativeMechanicsAndRemoveCompletely(String id, String parent) throws IOException {
        String path = "/Server/Entity/Effects/SolarRadiation/" + id + ".json";
        try (var stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            var asset = BsonDocument.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            assertEquals(parent, asset.getString("Parent").getValue());
            assertTrue(asset.getBoolean("Infinite").getValue());
            assertEquals("Ignore", asset.getString("OverlapBehavior").getValue());
            assertEquals("Complete", asset.getString("RemovalBehavior").getValue());
            assertFalse(asset.containsKey("DamageCalculator"), "Inherit native damage values");
        }
    }

    private void update(RadiationSubject subject, float dose) { reconcile(subject.symptoms(dose)); }

    private void reconcile(RadiationSymptoms symptoms) {
        RadiationEffects.update(null, controller, symptoms, null, assets);
    }

    private void seed(String id) {
        int index = assets.getIndex(id);
        controller.getActiveEffects().put(index, new ActiveEntityEffect(id, index, true, false));
    }

    private void assertActive(String... ids) {
        assertEquals(ids.length, controller.getActiveEffects().size());
        for (String id : ids) assertTrue(controller.hasEffect(assets.getIndex(id)), id);
    }

    private static final class TestEffect extends EntityEffect {
        TestEffect(String id, boolean poison) {
            super(id);
            infinite = true;
            removalBehavior = RemovalBehavior.COMPLETE;
            if (poison) damageCalculator = new DamageCalculator() {};
            damageCalculatorCooldown = 5;
        }

        void infinite(boolean value) { infinite = value; }
        void removal(RemovalBehavior value) { removalBehavior = value; }
        void cooldown(float value) { damageCalculatorCooldown = value; }
        void noDamage() { damageCalculator = null; }
    }

    private static final class TestAssets extends IndexedLookupTableAssetMap<String, EntityEffect> {
        final List<EntityEffect> effects = new ArrayList<>(List.of(new TestEffect(SLOW, false),
                new TestEffect(LOW, true), new TestEffect(HIGH, true),
                new TestEffect("Slow", false), new TestEffect("Poison_T1", true)));

        TestAssets() { super(EntityEffect[]::new); }

        @Override
        public int getIndex(String id) {
            for (int i = 0; i < effects.size(); i++) if (effects.get(i).getId().equals(id)) return i;
            return Integer.MIN_VALUE;
        }

        @Override
        public EntityEffect getAsset(String id) {
            int index = getIndex(id);
            return index < 0 ? null : getAsset(index);
        }

        @Override
        public EntityEffect getAsset(int index) { return effects.get(index); }
    }

    /** Records the native API contract without bootstrapping a Hytale server. */
    private static final class RecordingController extends EffectControllerComponent {
        private final TestAssets assets;
        final List<String> operations = new ArrayList<>();
        boolean rejectApplications;

        RecordingController(TestAssets assets) { this.assets = assets; }

        @Override
        public boolean addInfiniteEffect(Ref<EntityStore> ref, int index, EntityEffect effect,
                                         ComponentAccessor<EntityStore> accessor) {
            if (rejectApplications) return false;
            assertFalse(hasEffect(index), "An active effect must not be reapplied");
            activeEffects.put(index, new ActiveEntityEffect(effect.getId(), index, true, false));
            operations.add("add:" + effect.getId());
            return true;
        }

        @Override
        public void removeEffect(Ref<EntityStore> ref, int index, RemovalBehavior behavior,
                                 ComponentAccessor<EntityStore> accessor) {
            assertEquals(RemovalBehavior.COMPLETE, behavior);
            activeEffects.remove(index);
            operations.add("remove:" + assets.getAsset(index).getId());
        }
    }
}
