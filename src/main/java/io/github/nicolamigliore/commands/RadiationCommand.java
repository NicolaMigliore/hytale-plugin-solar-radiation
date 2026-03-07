package io.github.nicolamigliore.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class RadiationCommand extends AbstractCommandCollection {
    public RadiationCommand(){
        super("radiation", "Solar Radiation Debug Commands");
        addSubCommand(new RadiationSetCommand());
        addSubCommand(new RadiationAddCommand());
        addSubCommand(new RadiationStatsCommand());
    }
}
