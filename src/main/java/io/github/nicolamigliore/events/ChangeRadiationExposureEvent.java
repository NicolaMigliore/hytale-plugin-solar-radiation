package io.github.nicolamigliore.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public record ChangeRadiationExposureEvent(
        @Nonnull Ref<EntityStore> playerRef,
        boolean exposed
) implements IEvent<Void> {

    public static void dispatch(@Nonnull Ref<EntityStore> playerRef, boolean exposed){
        IEventDispatcher<ChangeRadiationExposureEvent, ChangeRadiationExposureEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(ChangeRadiationExposureEvent.class);

        if(dispatcher.hasListener()){
            dispatcher.dispatch(new ChangeRadiationExposureEvent(playerRef, exposed));
        }
    }
}