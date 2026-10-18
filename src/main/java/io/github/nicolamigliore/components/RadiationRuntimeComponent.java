package io.github.nicolamigliore.components;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.radiation.Exposure;
import javax.annotation.Nullable;

/** Temporary state for any entity with a dose; never written to an entity save. */
public class RadiationRuntimeComponent implements Component<EntityStore> {
    private static ComponentType<EntityStore, RadiationRuntimeComponent> type;

    private Exposure exposure = Exposure.UNKNOWN;
    private float exposureCheckRemaining;
    private int lastReportedStage = -1;

    public static void setComponentType(ComponentType<EntityStore, RadiationRuntimeComponent> componentType) {
        type = componentType;
    }

    public static ComponentType<EntityStore, RadiationRuntimeComponent> getComponentType() { return type; }
    public Exposure getExposure() { return exposure; }
    public void setExposure(Exposure value) { exposure = value; }
    public int getLastReportedStage() { return lastReportedStage; }
    public void setLastReportedStage(int stage) { lastReportedStage = stage; }

    public boolean shouldCheckExposure(float dt) {
        return shouldCheckExposure(dt, 0.5f);
    }

    public boolean shouldCheckExposure(float dt, float interval) {
        exposureCheckRemaining -= dt;
        if (exposureCheckRemaining > 0) return false;
        exposureCheckRemaining = interval;
        return true;
    }

    public void invalidateExposure() {
        exposure = Exposure.UNKNOWN;
        exposureCheckRemaining = 0;
    }

    @Override
    public RadiationRuntimeComponent clone() {
        // A clone can enter another world; sample exposure and feedback again there.
        return new RadiationRuntimeComponent();
    }

    @Override
    @Nullable
    public RadiationRuntimeComponent cloneSerializable() { return null; }
}
