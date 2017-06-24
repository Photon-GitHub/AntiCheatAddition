package de.photon.AACAdditionPro.command.subcommands;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.Permissions;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.command.subcommands.heuristics.CheckCommand;
import de.photon.AACAdditionPro.command.subcommands.heuristics.TrainCommand;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class HeuristicsCommand extends InternalCommand
{
    public HeuristicsCommand()
    {
        super("heuristics", (byte) 1, false, Permissions.NEURAL, "Everything regarding heuristics");
    }

    @Override
    protected void execute(final CommandSender sender, final LinkedList<String> arguments)
    {
        //Delegate the sub-commands
        if(AACAdditionPro.getInstance().getConfig().getBoolean("InventoryHeuristics.enabled")) {
            delegateToSubCommands(sender, arguments);
        }else {
            sender.sendMessage("InventoryHeuristics is not loaded / enabled.");
        }
    }

    @Override
    protected Set<InternalCommand> getChildCommands()
    {
        return new HashSet<>(Arrays.asList(
                new CheckCommand(),
                new TrainCommand())
        );
    }
}
