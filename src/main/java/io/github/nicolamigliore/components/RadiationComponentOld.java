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

public class RadiationComponentOld implements Component<EntityStore> {
    private int MAX_RADIATION_LEVEL = 5;
    private float rads = 0.0f;
    private int radiationLevel;
    private float radiationDecreaseDuration; // Time for radiation level to decrease naturally
    private float radiationDecreaseTimer = 0.0f;    // Time at current radiation Level

    private static ComponentType<EntityStore, RadiationComponentOld> type;
    public static ComponentType<EntityStore, RadiationComponentOld> getComponentType(){
        return type;
    }
    public static void setComponentType(ComponentType<EntityStore, RadiationComponentOld> type){
        RadiationComponentOld.type = type;
    }

    // Codec
    public static final BuilderCodec<RadiationComponentOld> CODEC =
            BuilderCodec.<RadiationComponentOld>builder(RadiationComponentOld.class, RadiationComponentOld::new)
            .append(new KeyedCodec<>("RadiationLevel", BuilderCodec.INTEGER),
                (
    data, value) -> data.radiationLevel = value,    // setter
    data -> data.radiationLevel     // getter
                )
            .add()
            .build();

    // Constructors
    public RadiationComponentOld(){
        this(0);
    }
    public RadiationComponentOld(int radiationLevel){
        this.radiationLevel = radiationLevel;
    }
    public RadiationComponentOld(RadiationComponentOld other){
        this.radiationLevel = other.radiationLevel;
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new RadiationComponentOld(this);
    }

    @NullableDecl
    @Override
    public Component<EntityStore> cloneSerializable() {
        return Component.super.cloneSerializable();
    }

    // Getters and Setters
    public float getRads(){ return this.rads; }
    public void setRads(float newRads){ this.rads = newRads; }
    public void addRads(float n){
        this.rads += n;

        if(this.rads >= 1.0f){
            if(this.radiationLevel < MAX_RADIATION_LEVEL){
                // Increase radiation level
                this.rads = 0.0f;
                this.addRadiationLevel(1);
            }else{
                // Cap rads if max radiation level
                this.rads = 1.0f;
            }
        }else if(this.rads <= 0.0f){
            if(this.radiationLevel > 0){
                // Decrease radiation level
                this.rads = 1.0f;
                this.addRadiationLevel(-1);
            }else{
                // Cap rads if min radiation level
                this.rads = 0.0f;
            }
        }
    }
    public int getRadiationLevel(){ return this.radiationLevel; }
    public void setRadiationLevel(int newLevel){ this.radiationLevel = newLevel; }
    public void addRadiationLevel(int n){ this.radiationLevel += n; }
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
