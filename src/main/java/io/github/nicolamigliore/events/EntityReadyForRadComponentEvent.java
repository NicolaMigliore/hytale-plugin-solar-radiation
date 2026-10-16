package io.github.nicolamigliore.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.components.RadiationComponentOld;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class EntityReadyForRadComponentEvent {
    public static void onPlayerReady(PlayerReadyEvent event) {
        Ref<EntityStore> playerRef = event.getPlayerRef();
        PlayerRef recipient = playerRef.getStore().getComponent(playerRef, PlayerRef.getComponentType());
        if (recipient == null) return;
        recipient.sendMessage(Message.raw(recipient.getUsername() + " is ready for a radiation component"));
        recipient.sendMessage(Message.raw("ref from event: " + playerRef.toString()));

        // Add RadiationComponent if entity does not have it.
        Store<EntityStore> store = playerRef.getStore();
        RadiationComponentOld radComponent = store.getComponent(playerRef, RadiationComponentOld.getComponentType());

        if(radComponent == null){
            // Create radiation component
            radComponent = new RadiationComponentOld(0);

            // Configure radiation component
            radComponent.setDuration(5.0f);
            radComponent.setTimer(0.0f);

            // Add component
            store.addComponent(playerRef, RadiationComponentOld.getComponentType(), radComponent);
        }
    }
}
