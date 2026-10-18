package io.github.nicolamigliore.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.components.RadiationComponent;
import io.github.nicolamigliore.components.RadiationRuntimeComponent;
import io.github.nicolamigliore.radiation.Exposure;
import javax.annotation.Nonnull;

public class RadiationStatsCommand extends AbstractPlayerCommand {
    private final FlagArg target;

    public RadiationStatsCommand() {
        super("stats", "Show dose, stage, and solar exposure");
        target = withFlagArg("target", "Inspect the player or mob you are looking at");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef recipient, @Nonnull World world) {
        var entity = RadiationTarget.resolve(ref, Boolean.TRUE.equals(target.get(context)), store, recipient);
        if (entity == null) return;
        var radiation = store.getComponent(entity, RadiationComponent.getComponentType());
        if (radiation == null) {
            recipient.sendMessage(Message.raw("This entity has no radiation data."));
            return;
        }
        recipient.sendMessage(Message.raw("Radiation dose: %.2f / 180 | stage %d".formatted(
                radiation.getTotalRadiations(), radiation.getRadLevel())));
        var runtime = store.getComponent(entity, RadiationRuntimeComponent.getComponentType());
        recipient.sendMessage(Message.raw("Exposure / recovery: "
                + (runtime == null ? Exposure.UNKNOWN : runtime.getExposure())));
        recipient.sendMessage(Message.raw("Next stage: %.2f dose remaining | progress %.1f%%".formatted(
                radiation.getRadiationsToNextLevel(), radiation.getProgress() * 100)));
    }
}
