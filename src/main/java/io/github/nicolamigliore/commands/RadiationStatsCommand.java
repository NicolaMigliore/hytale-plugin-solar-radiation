package io.github.nicolamigliore.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.components.RadiationComponent;
import javax.annotation.Nonnull;

public class RadiationStatsCommand extends AbstractPlayerCommand {
    public RadiationStatsCommand() { super("stats", "Show dose, stage, and solar exposure"); }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef recipient, @Nonnull World world) {
        var radiation = store.getComponent(ref, RadiationComponent.getComponentType());
        if (radiation == null) {
            recipient.sendMessage(Message.raw("Radiation data is not ready yet."));
            return;
        }
        recipient.sendMessage(Message.raw("Radiation dose: %.2f / 180 | stage %d".formatted(
                radiation.getTotalRadiations(), radiation.getRadLevel())));
        recipient.sendMessage(Message.raw("Solar exposure: " + radiation.getExposure()));
        recipient.sendMessage(Message.raw("Next stage: %.2f dose remaining | progress %.1f%%".formatted(
                radiation.getRadiationsToNextLevel(), radiation.getProgress() * 100)));
    }
}
