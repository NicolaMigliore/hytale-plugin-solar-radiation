package io.github.nicolamigliore.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public record ChangeRadiationEvent(
        @Nonnull Ref<EntityStore> playerRef,
        float amount
) implements IEvent<Void> {

    public static void dispatch(Ref<EntityStore> playerRef, float amount){
        IEventDispatcher<ChangeRadiationEvent, ChangeRadiationEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(ChangeRadiationEvent.class);

        if(dispatcher.hasListener()){
            dispatcher.dispatch(new ChangeRadiationEvent(playerRef, amount));
        }
    }
}
