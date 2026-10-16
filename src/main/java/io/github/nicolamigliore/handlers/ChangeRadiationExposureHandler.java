package io.github.nicolamigliore.handlers;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.components.RadiationComponent;
import io.github.nicolamigliore.events.ChangeRadiationExposureEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.function.Consumer;

public class ChangeRadiationExposureHandler implements Consumer<ChangeRadiationExposureEvent> {

    @Override
    public void accept(ChangeRadiationExposureEvent event) {
        if(!event.playerRef().isValid()) return;

        Store<EntityStore> store = event.playerRef().getStore();
        RadiationComponent radiationComponent = store.getComponent(event.playerRef(), RadiationComponent.getComponentType());
        if(radiationComponent == null) return;

        PlayerRef playerRef = store.getComponent(event.playerRef(), PlayerRef.getComponentType());
        if (playerRef != null) playerRef.sendMessage(Message.raw("Exposure changed: %b".formatted(event.exposed())));

        radiationComponent.setExposedToRadiation(event.exposed());
    }
}
