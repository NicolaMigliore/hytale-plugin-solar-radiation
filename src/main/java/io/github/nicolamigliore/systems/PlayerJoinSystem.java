package io.github.nicolamigliore.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.components.RadiationComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class PlayerJoinSystem extends RefSystem<EntityStore> {

    @Override
    public void onEntityAdded(
            @NullableDecl Ref<EntityStore> ref,
            @NonNullDecl AddReason addReason,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl CommandBuffer<EntityStore> commandBuffer
    ){
        if (addReason != AddReason.LOAD) return;

        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;

        var radiationComponentType = RadiationComponent.getComponentType();
        var radiation = store.getComponent(ref, radiationComponentType);

        if(radiation != null){
            playerRef.sendMessage(Message.raw(
                    "Welcome back to the wasteland! Current Radiation Level %d (%.2f radiations)".formatted(radiation.getRadLevel(), radiation.getTotalRadiations())
            ));
        }else{
            commandBuffer.addComponent(ref, radiationComponentType, new RadiationComponent());
            playerRef.sendMessage(Message.raw("Welcome to the wasteland!"));
        }


    }

    @Override
    public void onEntityRemove(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl RemoveReason removeReason, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery(){
        return Archetype.of(PlayerRef.getComponentType());
    }
}
