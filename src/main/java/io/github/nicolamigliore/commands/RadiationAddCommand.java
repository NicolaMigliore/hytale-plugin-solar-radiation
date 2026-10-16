package io.github.nicolamigliore.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.components.RadiationComponent;
import io.github.nicolamigliore.events.ChangeRadiationEvent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;

public class RadiationAddCommand extends AbstractPlayerCommand {
    private static final int DEFAULT_AMOUNT = 50;

    private final OptionalArg<Integer> amountArg;

    public RadiationAddCommand(){
        super("add", "Add to your radiations");
        this.amountArg = withOptionalArg("amount", "Radiation amount to add", ArgTypes.INTEGER);
    }

    @Override
    protected void execute(
            @NonNullDecl CommandContext commandContext,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world
    ) {
        var amount = this.amountArg.get(commandContext);
        if (amount == null) amount = DEFAULT_AMOUNT;

        RadiationComponent radComponent = store.getComponent(ref, RadiationComponent.getComponentType());

        if(radComponent == null){
            // Create radiation component
            radComponent = new RadiationComponent(0);

            // Add component
            store.addComponent(ref, RadiationComponent.getComponentType(), radComponent);
            playerRef.sendMessage(Message.raw("Added new Radiation Component"));
        }

        // Add radiation amount
        ChangeRadiationEvent.dispatch(ref, amount);

        playerRef.sendMessage(Message.raw("+%d radiations".formatted(amount))
                .color(Color.GREEN)
                .bold(true));
    }
}
