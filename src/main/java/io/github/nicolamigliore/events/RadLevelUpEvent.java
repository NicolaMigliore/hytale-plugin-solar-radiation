package io.github.nicolamigliore.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public record RadLevelUpEvent(
        @Nonnull Ref<EntityStore> playerRef,
        int oldLevel,
        int newLevel
) implements IEvent<Void> {

    public int radLevelsGained(){
        return newLevel - oldLevel;
    }

    public static void dispatch(Ref<EntityStore> playerRef, int oldLevel, int newLevel){
        IEventDispatcher<RadLevelUpEvent, RadLevelUpEvent> dispatcher = HytaleServer.get().getEventBus().dispatchFor(RadLevelUpEvent.class);

        if(dispatcher.hasListener()){
            dispatcher.dispatch(new RadLevelUpEvent(playerRef, oldLevel, newLevel));
        }
    }
}