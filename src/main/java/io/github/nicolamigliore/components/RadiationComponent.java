package io.github.nicolamigliore.components;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import javax.annotation.Nullable;

// Info: As reference check:
//      - com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent
//      - https://hytalemodding.dev/en/docs/guides/ecs/example-ecs-plugin

public class RadiationComponent implements Component<EntityStore> {
    private int radiationLevel;
    private float radiationDecreaseDuration; // Time for radiation level to decrease naturally
    private float radiationDecreaseTimer = 0.0f;    // Time at current radiation Level

    private static ComponentType<EntityStore, RadiationComponent> type;
    public static ComponentType<EntityStore, RadiationComponent> getComponentType(){
        return type;
    }
    public static void setComponentType(ComponentType<EntityStore, RadiationComponent> type){
        RadiationComponent.type = type;
    }

    // Codec
    public static final BuilderCodec<RadiationComponent> CODEC =
            BuilderCodec.<RadiationComponent>builder(RadiationComponent.class, RadiationComponent::new)
            .append(new KeyedCodec<>("RadiationLevel", BuilderCodec.INTEGER),
                (
    data, value) -> data.radiationLevel = value,    // setter
    data -> data.radiationLevel     // getter
                )
            .add()
            .build();

    // Constructors
    public RadiationComponent(){
        this(0);
    }
    public RadiationComponent(int radiationLevel){
        this.radiationLevel = radiationLevel;
    }
    public RadiationComponent(RadiationComponent other){
        this.radiationLevel = other.radiationLevel;
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new RadiationComponent(this);
    }

    @NullableDecl
    @Override
    public Component<EntityStore> cloneSerializable() {
        return Component.super.cloneSerializable();
    }

    // Getters and Setters
    public int getRadiationLevel(){ return this.radiationLevel; }
    public void setRadiationLevel(int newLevel){ this.radiationLevel = newLevel; }
    public float getDuration() {
        return this.radiationDecreaseDuration;
    }
    public void setDuration(float radiationDecreaseDuration) {
        this.radiationDecreaseDuration = radiationDecreaseDuration;
    }
    public float getTimer() {
        return this.radiationDecreaseTimer;
    }
    public void incrementTimer(float time) { this.radiationDecreaseTimer += time; }
    public void setTimer(float time) { this.radiationDecreaseTimer = time; }
}
