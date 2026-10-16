package io.github.nicolamigliore.events;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class ExampleEvent {

    public static void onPlayerReady(PlayerReadyEvent event) {
        var ref = event.getPlayerRef();
        PlayerRef playerRef = ref.getStore().getComponent(ref, PlayerRef.getComponentType());
        if (playerRef != null) playerRef.sendMessage(Message.raw("Welcome " + playerRef.getUsername()));
    }

}
