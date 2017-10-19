package de.photon.AACAdditionPro.command.subcommands;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.command.subcommands.heuristics.CheckCommand;
import de.photon.AACAdditionPro.command.subcommands.heuristics.TrainCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class HeuristicsCommand extends InternalCommand
{
    public HeuristicsCommand()
    {
        super("heuristics", InternalPermission.NEURAL, (byte) 1);
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        if (!AACAdditionPro.getInstance().getConfig().getBoolean("InventoryHeuristics.enabled"))
        {
            sender.sendMessage(prefix + ChatColor.RED + "InventoryHeuristics is not loaded / enabled.");
        }
    }

    @Override
    protected String[] getCommandHelp()
    {
        return new String[]{"Utilities for the InventoryHeuristics"};
    }

    @Override
    protected Set<InternalCommand> getChildCommands()
    {
        return new HashSet<>(Arrays.asList(
                new CheckCommand(),
                new TrainCommand())
        );
    }

    @Override
    protected String[] getTabPossibilities()
    {
        return getChildTabs();
    }
}
