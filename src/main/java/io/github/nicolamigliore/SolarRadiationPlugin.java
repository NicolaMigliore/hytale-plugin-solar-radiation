package io.github.nicolamigliore;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import io.github.nicolamigliore.commands.RadiationCommand;
import io.github.nicolamigliore.components.RadiationComponent;
import io.github.nicolamigliore.radiation.RadiationEffects;
import io.github.nicolamigliore.radiation.RadiationSettings;
import io.github.nicolamigliore.systems.PlayerJoinSystem;
import io.github.nicolamigliore.systems.RadiationDeathSystem;
import io.github.nicolamigliore.systems.RadiationSystem;
import javax.annotation.Nonnull;

public class SolarRadiationPlugin extends JavaPlugin {
    private final Config<RadiationSettings> settings;

    public SolarRadiationPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        settings = withConfig("Radiation", RadiationSettings.CODEC);
    }

    @Override
    protected void setup() {
        settings.save();
        var registry = getEntityStoreRegistry();
        RadiationComponent.setComponentType(registry.registerComponent(
                RadiationComponent.class, "SolarRadiation_EntityData", RadiationComponent.CODEC));
        registry.registerSystem(new PlayerJoinSystem());
        registry.registerSystem(new RadiationSystem(settings.get()));
        registry.registerSystem(new RadiationDeathSystem());
        getCommandRegistry().registerCommand(new RadiationCommand());
    }

    @Override
    protected void start() {
        RadiationEffects.validateAssets();
    }
}
