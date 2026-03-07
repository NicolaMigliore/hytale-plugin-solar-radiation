package io.github.nicolamigliore.handlers;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.events.RadLevelUpEvent;

import java.util.function.Consumer;

public class RadLevelUpHandler implements Consumer<RadLevelUpEvent> {
    @Override
    public void accept(RadLevelUpEvent event) {
        if(!event.playerRef().isValid()) return;

        Store<EntityStore> store = event.playerRef().getStore();
        var playerRef = store.getComponent(event.playerRef(), PlayerRef.getComponentType());
        if(playerRef == null) return;

        var message = switch (event.radLevelsGained()){
            case 1 -> "RAD LEVEL INCREASED! You are now at %d radiation poisoning level".formatted(event.newLevel());
            default -> "RAD LEVEL INCREASED! +%d rad levels! Now at %d radiation poisoning level".formatted(event.radLevelsGained(), event.newLevel());
        };

        playerRef.sendMessage(Message.raw(message));

    }
}
