package io.github.nicolamigliore.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.radiation.RadLevelTable;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class RadiationComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, RadiationComponent> TYPE;

    public static void setComponentType(ComponentType<EntityStore, RadiationComponent> type){
        TYPE = type;
    }

    public static ComponentType<EntityStore, RadiationComponent> getComponentType(){
        return TYPE;
    }

    public static final BuilderCodec<RadiationComponent> CODEC = BuilderCodec
            .builder(RadiationComponent.class, RadiationComponent::new)
            .append(
                    new KeyedCodec<>("TotalRadiations", Codec.FLOAT),
                    (component, value) -> component.totalRadiations = value,
                    component -> component.totalRadiations
            )
            .add()
            .append(
                    new KeyedCodec<>("ExposedToRadiation", Codec.BOOLEAN),
                    (component, value) -> component.exposedToRadiation = value,
                    component -> component.exposedToRadiation
            )
            .add()
            .build();

    private float totalRadiations = 0f;
    private boolean exposedToRadiation = false;

    public RadiationComponent() {}

    public RadiationComponent(float totalRadiations){
        this.totalRadiations = Math.max(0f, totalRadiations);
    }

    public RadiationComponent(float totalRadiations, boolean exposedToRadiation){
        this.totalRadiations = Math.max(0f, totalRadiations);
        this.exposedToRadiation = exposedToRadiation;
    }

    public float getTotalRadiations() {
        return totalRadiations;
    }

    public void setTotalRadiations(float totalRadiations){
        this.totalRadiations = Math.max(0f, totalRadiations);
    }

    public boolean isExposedToRadiation() {
        return exposedToRadiation;
    }

    public void setExposedToRadiation(boolean exposedToRadiation) {
        this.exposedToRadiation = exposedToRadiation;
    }

    public int getRadLevel() {
        return RadLevelTable.getRadLevelForRads((long) totalRadiations);
    }

    public long getCurrentRadLevelRadiations() {
        return RadLevelTable.getRadsInCurrentRadLevel((long) totalRadiations);
    }

    public long getRadiationsToNextLevel() {
        return RadLevelTable.getRadsToNextRadLevel((long) totalRadiations);
    }

    public float getProgress() {
        return RadLevelTable.getProgressToNextRadLevel((long) totalRadiations);
    }

    public boolean isMaxRadLevel(){
        return getRadLevel() >= RadLevelTable.MAX_RAD_LEVEL;
    }

    public boolean addRadiations(float amount){
        int oldRadLevel = getRadLevel();
        totalRadiations = Math.max(0f, totalRadiations + amount);
        int newRadLevel = getRadLevel();
        return newRadLevel > oldRadLevel;
    }

    @NullableDecl
    @Override
    public RadiationComponent clone(){
        return new RadiationComponent(this.totalRadiations, this.exposedToRadiation);
    }

    @Override
    public String toString(){
        return "RadiationComponent{" +
                "radLevel=" + getRadLevel() +
                ", totalRads=" + totalRadiations +
                ", toNextRadLevel=" + getRadiationsToNextLevel() +
                ", exposed=" + exposedToRadiation +
                "}";
    }
}