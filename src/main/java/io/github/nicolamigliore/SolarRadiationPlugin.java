package io.github.nicolamigliore;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import io.github.nicolamigliore.commands.RadiationCommand;
import io.github.nicolamigliore.components.RadiationComponent;
import io.github.nicolamigliore.components.RadiationRuntimeComponent;
import io.github.nicolamigliore.radiation.RadiationEffects;
import io.github.nicolamigliore.radiation.RadiationSettings;
import io.github.nicolamigliore.systems.PlayerJoinSystem;
import io.github.nicolamigliore.systems.RadiationDeathSystem;
import io.github.nicolamigliore.systems.RadiationEntitySystem;
import io.github.nicolamigliore.systems.RadiationComponentSystem;
import io.github.nicolamigliore.systems.RadiationSystem;
import io.github.nicolamigliore.systems.RadiationHudSystem;
import io.github.nicolamigliore.ui.RadiationHud;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.Universe;
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
        RadiationRuntimeComponent.setComponentType(registry.registerComponent(
                RadiationRuntimeComponent.class, RadiationRuntimeComponent::new));
        registry.registerSystem(new RadiationEntitySystem());
        registry.registerSystem(new RadiationComponentSystem());
        registry.registerSystem(new PlayerJoinSystem());
        registry.registerSystem(new RadiationSystem(settings.get()));
        registry.registerSystem(new RadiationDeathSystem());
        registry.registerSystem(new RadiationHudSystem());
        getCommandRegistry().registerCommand(new RadiationCommand());
    }

    @Override
    protected void start() {
        RadiationEffects.validateAssets();
    }

    @Override
    protected void shutdown() {
        var universe = Universe.get();
        if (universe == null) return;
        for (var world : universe.getWorlds().values()) {
            if (!world.isAlive()) continue;
            world.execute(() -> {
                for (var playerRef : world.getPlayerRefs()) {
                    var ref = playerRef.getReference();
                    if (ref == null || !ref.isValid() || ref.getStore().getExternalData().getWorld() != world) continue;
                    var player = ref.getStore().getComponent(ref, Player.getComponentType());
                    if (player != null) RadiationHud.remove(player.getHudManager(), playerRef);
                }
            });
        }
    }
}
