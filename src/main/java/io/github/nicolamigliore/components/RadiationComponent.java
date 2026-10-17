package io.github.nicolamigliore.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.radiation.Exposure;
import io.github.nicolamigliore.radiation.RadLevelTable;

public class RadiationComponent implements Component<EntityStore> {
    private static ComponentType<EntityStore, RadiationComponent> type;

    // Keep the original dose key and codec type so existing player saves migrate.
    public static final BuilderCodec<RadiationComponent> CODEC = BuilderCodec
            .builder(RadiationComponent.class, RadiationComponent::new)
            .append(new KeyedCodec<>("TotalRadiations", Codec.FLOAT),
                    RadiationComponent::setTotalRadiations, RadiationComponent::getTotalRadiations)
            .add()
            .build();

    private float totalRadiations;
    private Exposure exposure = Exposure.UNKNOWN;
    private float exposureCheckRemaining;
    private int lastReportedStage = -1;

    public RadiationComponent() {}

    public RadiationComponent(float dose) {
        setTotalRadiations(dose);
    }

    public static void setComponentType(ComponentType<EntityStore, RadiationComponent> componentType) {
        type = componentType;
    }

    public static ComponentType<EntityStore, RadiationComponent> getComponentType() {
        return type;
    }

    public float getTotalRadiations() { return totalRadiations; }
    public void setTotalRadiations(float dose) { totalRadiations = RadLevelTable.clampDose(dose); }
    public int getRadLevel() { return RadLevelTable.getRadLevelForRads(totalRadiations); }
    public float getProgress() { return RadLevelTable.getProgressToNextRadLevel(totalRadiations); }
    public float getRadiationsToNextLevel() { return RadLevelTable.getRadsToNextRadLevel(totalRadiations); }
    public Exposure getExposure() { return exposure; }
    public void setExposure(Exposure value) { exposure = value; }
    public int getLastReportedStage() { return lastReportedStage; }
    public void setLastReportedStage(int stage) { lastReportedStage = stage; }

    public boolean shouldCheckExposure(float dt) {
        exposureCheckRemaining -= dt;
        if (exposureCheckRemaining > 0) return false;
        exposureCheckRemaining = 0.5f;
        return true;
    }

    public void invalidateExposure() {
        exposure = Exposure.UNKNOWN;
        exposureCheckRemaining = 0;
    }

    @Override
    public RadiationComponent clone() {
        // Exposure is local to the current world and is always resampled on load/transfer.
        return new RadiationComponent(totalRadiations);
    }
}
