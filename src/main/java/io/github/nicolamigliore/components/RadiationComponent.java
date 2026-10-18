package io.github.nicolamigliore.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.radiation.RadLevelTable;

/** Persistent entity dose. Exposure and presentation belong to RadiationRuntimeComponent. */
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
    @Override
    public RadiationComponent clone() {
        return new RadiationComponent(totalRadiations);
    }
}
