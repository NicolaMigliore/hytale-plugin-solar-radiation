package io.github.nicolamigliore;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.commands.RadiationSetCommand;
import io.github.nicolamigliore.components.RadiationComponent;
import io.github.nicolamigliore.systems.RadiationSystem;

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
        this.getCommandRegistry().registerCommand(new RadiationSetCommand());

        // Register events
        // TODO: Implement...

        // Register the component & set the type in the component class so it's easier to retrieve.
        ComponentType<EntityStore, RadiationComponent> radiationComponentComponentType = this.getEntityStoreRegistry()
                .registerComponent(RadiationComponent.class, RadiationComponent::new);
        RadiationComponent.setComponentType(radiationComponentComponentType);

        // Register System
        this.getEntityStoreRegistry().registerSystem(new RadiationSystem(RadiationComponent.getComponentType()));
    }

    public static SolarRadiationPlugin get() {
        return instance;
    }
}