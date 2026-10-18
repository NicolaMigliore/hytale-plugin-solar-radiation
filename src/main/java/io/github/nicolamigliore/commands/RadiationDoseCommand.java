package io.github.nicolamigliore.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.components.RadiationComponent;
import javax.annotation.Nonnull;

abstract class RadiationDoseCommand extends AbstractPlayerCommand {
    private final boolean set;
    private final OptionalArg<Float> amount;
    private final FlagArg target;

    protected RadiationDoseCommand(String name, boolean set) {
        super(name, set ? "Set radiation dose (0 to 180)" : "Add radiation dose");
        this.set = set;
        amount = withOptionalArg("amount", "Dose amount", ArgTypes.FLOAT);
        target = withFlagArg("target", "Apply to the player or mob you are looking at");
        requirePermission("solarradiation.admin");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef recipient, @Nonnull World world) {
        Float value = amount.get(context);
        if (value == null && set) {
            recipient.sendMessage(Message.raw("Usage: /radiation set --amount=100"));
            return;
        }
        if (value == null) value = 50f;
        if (!Float.isFinite(value)) {
            recipient.sendMessage(Message.raw("Radiation amount must be a finite number."));
            return;
        }
        var entity = RadiationTarget.resolve(ref, Boolean.TRUE.equals(target.get(context)), store, recipient);
        if (entity == null) return;
        var radiation = store.getComponent(entity, RadiationComponent.getComponentType());
        if (radiation == null) {
            radiation = new RadiationComponent();
            store.addComponent(entity, RadiationComponent.getComponentType(), radiation);
        }
        radiation.setTotalRadiations(set ? value : radiation.getTotalRadiations() + value);
        recipient.sendMessage(Message.raw("Radiation dose: %.2f / 180 | stage %d".formatted(
                radiation.getTotalRadiations(), radiation.getRadLevel())));
    }
}
