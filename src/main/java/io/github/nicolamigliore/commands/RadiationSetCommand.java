package io.github.nicolamigliore.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.components.RadiationComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.awt.*;

public class RadiationSetCommand extends AbstractPlayerCommand {
    @Nonnull
    private final RequiredArg<Integer> radLevelArg = this.withRequiredArg("radlevel", "New radiation level", ArgTypes.INTEGER);

    public RadiationSetCommand(){
        super("radiationset", "Super radiation set command!");
    }

    @Override
    protected void execute(
            @NonNullDecl CommandContext commandContext,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world
    ) {
        int radLevel = this.radLevelArg.get(commandContext);
        setEntityRadiationLevel(ref, store, radLevel);
    }

    public static void setEntityRadiationLevel(
            @NonNullDecl Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            int radiationLevel
    ){
        // Get radiation component
        RadiationComponent radComponent = store.getComponent(ref, RadiationComponent.getComponentType());

        if(radComponent == null){
            // Create radiation component
            radComponent = new RadiationComponent(radiationLevel);

            // Configure radiation component
            radComponent.setDuration(5.0f);
            radComponent.setTimer(0.0f);

            // Add component
            store.addComponent(ref, RadiationComponent.getComponentType(), radComponent);
        }

        // Set radiation level
        radComponent.setRadiationLevel(radiationLevel);

        // Get entity UUID
        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());

        Player player = store.getComponent(ref, Player.getComponentType());
        player.sendMessage(Message.raw("Your id is: " + uuidComponent.getUuid() + " radiation level is: " + radComponent.getRadiationLevel())
                .color(Color.GREEN)
                .bold(true));
    }
}
