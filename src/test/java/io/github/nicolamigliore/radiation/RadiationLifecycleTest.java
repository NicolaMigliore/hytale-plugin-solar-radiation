package io.github.nicolamigliore.radiation;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.components.RadiationComponent;
import io.github.nicolamigliore.components.RadiationRuntimeComponent;
import io.github.nicolamigliore.systems.RadiationComponentSystem;
import io.github.nicolamigliore.systems.RadiationEntitySystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Uses Hytale's actual component registry, holders, stores, and lifecycle callbacks. */
class RadiationLifecycleTest {
    private ComponentRegistry<EntityStore> registry;
    private ComponentType<EntityStore, RadiationComponent> doseType;
    private ComponentType<EntityStore, RadiationRuntimeComponent> runtimeType;

    @BeforeEach
    void registerComponents() {
        registry = new ComponentRegistry<>();
        doseType = registry.registerComponent(RadiationComponent.class, "SolarRadiation_EntityData", RadiationComponent.CODEC);
        runtimeType = registry.registerComponent(RadiationRuntimeComponent.class, RadiationRuntimeComponent::new);
        RadiationComponent.setComponentType(doseType);
        RadiationRuntimeComponent.setComponentType(runtimeType);
        var effectsType = registry.registerComponent(EffectControllerComponent.class, EffectControllerComponent::new);
        registry.registerSystem(new RadiationEntitySystem());
        registry.registerSystem(new RadiationComponentSystem(effectsType));
    }

    @AfterEach
    void shutdown() { if (registry != null) registry.shutdown(); }

    @Test
    void savingAndLoadingANonPlayerRetainsOnlyDose() {
        var store = registry.addStore(null, EmptyResourceStorage.get());
        var entity = registry.newHolder();
        entity.addComponent(doseType, new RadiationComponent(89.5f));
        var runtime = new RadiationRuntimeComponent();
        runtime.setExposure(Exposure.EXPOSED);
        runtime.setLastReportedStage(3);
        entity.addComponent(runtimeType, runtime);
        var saved = entity.cloneSerializable(registry.getData());
        assertEquals(89.5f, saved.getComponent(doseType).getTotalRadiations());
        assertNull(saved.getComponent(runtimeType));

        var ref = store.addEntity(saved, AddReason.LOAD);
        var restored = store.getComponent(ref, runtimeType);
        assertNotNull(restored);
        assertEquals(Exposure.UNKNOWN, restored.getExposure());
        assertEquals(-1, restored.getLastReportedStage());
        assertEquals(89.5f, store.getComponent(ref, doseType).getTotalRadiations());
    }

    @Test
    void aLiveNonPlayerCanOptInWithoutAnyPlayerComponent() {
        var store = registry.addStore(null, EmptyResourceStorage.get());
        var ref = store.addEntity(registry.newHolder(), AddReason.SPAWN);
        store.addComponent(ref, doseType, new RadiationComponent(10));
        assertNotNull(store.getComponent(ref, runtimeType));
        assertEquals(10f, store.getComponent(ref, doseType).getTotalRadiations());
    }

    @Test
    void replacingDoseDoesNotResetRuntimeButRemovingDoseCleansItUp() {
        var store = registry.addStore(null, EmptyResourceStorage.get());
        var entity = registry.newHolder();
        entity.addComponent(doseType, new RadiationComponent(10));
        var ref = store.addEntity(entity, AddReason.SPAWN);
        var runtime = store.getComponent(ref, runtimeType);
        runtime.setExposure(Exposure.RECOVERING);
        store.replaceComponent(ref, doseType, new RadiationComponent(20));
        assertSame(runtime, store.getComponent(ref, runtimeType));
        assertEquals(Exposure.RECOVERING, runtime.getExposure());

        store.removeComponent(ref, doseType);
        assertNull(store.getComponent(ref, runtimeType));
        store.addComponent(ref, doseType, new RadiationComponent(5));
        assertEquals(Exposure.UNKNOWN, store.getComponent(ref, runtimeType).getExposure());
    }
}
