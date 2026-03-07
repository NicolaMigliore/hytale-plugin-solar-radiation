package io.github.nicolamigliore;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.blockhealth.BlockHealthModule;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.commands.RadiationCommand;
import io.github.nicolamigliore.commands.RadiationSetCommand;
import io.github.nicolamigliore.components.RadiationComponent;
import io.github.nicolamigliore.components.RadiationComponentOld;
import io.github.nicolamigliore.events.ChangeRadiationEvent;
import io.github.nicolamigliore.events.ChangeRadiationExposureEvent;
import io.github.nicolamigliore.events.EntityReadyForRadComponentEvent;
import io.github.nicolamigliore.events.RadLevelUpEvent;
import io.github.nicolamigliore.handlers.ChangeRadiationExposureHandler;
import io.github.nicolamigliore.handlers.ChangeRadiationHandler;
import io.github.nicolamigliore.handlers.RadLevelUpHandler;
import io.github.nicolamigliore.systems.PlayerJoinSystem;
import io.github.nicolamigliore.systems.RadiationSystem;
import io.github.nicolamigliore.systems.SunlightExposureSystem;

import javax.annotation.Nonnull;

public class SolarRadiationPlugin extends JavaPlugin {

    private static SolarRadiationPlugin instance;

    public SolarRadiationPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        // this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, ExampleEvent::onPlayerReady);

        // Register commands
        // this.getCommandRegistry().registerCommand(new ExampleCommand("example", "An example command"));
//        this.getCommandRegistry().registerCommand(new RadiationSetCommand());
        this.getCommandRegistry().registerCommand(new RadiationCommand());

        // Register events
//        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, EntityReadyForRadComponentEvent::onPlayerReady);

        getEventRegistry().register(ChangeRadiationEvent.class, new ChangeRadiationHandler());
        getEventRegistry().register(RadLevelUpEvent.class, new RadLevelUpHandler());
        getEventRegistry().register(ChangeRadiationExposureEvent.class, new ChangeRadiationExposureHandler());

        var entityRegistry = this.getEntityStoreRegistry();
        // Register the component & set the type in the component class so it's easier to retrieve.
        ComponentType<EntityStore, RadiationComponent> radiationComponentComponentType = entityRegistry.registerComponent(
                RadiationComponent.class,
                "SolarRadiation_EntityData",
                RadiationComponent.CODEC
        );
        RadiationComponent.setComponentType(radiationComponentComponentType);
//        ComponentType<EntityStore, RadiationComponentOld> radiationComponentComponentType = this.getEntityStoreRegistry()
//                .registerComponent(RadiationComponentOld.class, RadiationComponentOld::new);
//        RadiationComponentOld.setComponentType(radiationComponentComponentType);

        // Register System
        entityRegistry.registerSystem(new PlayerJoinSystem());
        entityRegistry.registerSystem(new SunlightExposureSystem());
        entityRegistry.registerSystem(new RadiationSystem(RadiationComponent.getComponentType()));
    }

    public static SolarRadiationPlugin get() {
        return instance;
    }
}