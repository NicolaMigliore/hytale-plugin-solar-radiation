package io.github.nicolamigliore.handlers;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.components.RadiationComponent;
import io.github.nicolamigliore.events.ChangeRadiationEvent;
import io.github.nicolamigliore.events.RadLevelUpEvent;

import java.util.function.Consumer;

public class ChangeRadiationHandler implements Consumer<ChangeRadiationEvent> {

    @Override
    public void accept(ChangeRadiationEvent event) {
        if(!event.playerRef().isValid()) return;

        Store<EntityStore> store = event.playerRef().getStore();
        RadiationComponent radiationComponent = store.getComponent(event.playerRef(), RadiationComponent.getComponentType());
        if(radiationComponent == null) return;

        Player player = store.getComponent(event.playerRef(), Player.getComponentType());
//        player.sendMessage(Message.raw("Adding radiations: %.2f".formatted(event.amount())));

        int oldRadLevel = radiationComponent.getRadLevel();
        var increasedRadLevel = radiationComponent.addRadiations(event.amount());

        // TODO: change to handle also rad level decrease
        if(increasedRadLevel){
            RadLevelUpEvent.dispatch(event.playerRef(), oldRadLevel, radiationComponent.getRadLevel());
        }

    }
}
