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
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class RadiationStatsCommand extends AbstractPlayerCommand {
    public RadiationStatsCommand(){
        super("stats", "Show your radiation stats");
    }

    @Override
    protected void execute(
            @NonNullDecl CommandContext commandContext,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world
    ) {
        RadiationComponent radiationComponent = store.getComponent(ref, RadiationComponent.getComponentType());
        if(radiationComponent == null){
            playerRef.sendMessage(Message.raw("No Radiation component found"));
            return;
        }

        int radLevel = radiationComponent.getRadLevel();
        float totalRadiations = radiationComponent.getTotalRadiations();
        long toNextRadLevel = radiationComponent.getRadiationsToNextLevel();
        float progress = radiationComponent.getProgress();
        boolean isExposed = radiationComponent.isExposedToRadiation();

        playerRef.sendMessage(Message.raw("=== Radiation Stats ==="));
        playerRef.sendMessage(Message.raw("Radiation Level: %d".formatted(radLevel)));
        playerRef.sendMessage(Message.raw("Progress to next Radiation Level: %.2f".formatted(progress)));
        playerRef.sendMessage(Message.raw("Total Radiations: %.2f".formatted(totalRadiations)));
        playerRef.sendMessage(Message.raw("Currently Exposed: %b".formatted(isExposed)));

    }
}
