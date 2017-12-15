package de.photon.AACAdditionPro.command.subcommands;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.command.subcommands.heuristics.CreateCommand;
import de.photon.AACAdditionPro.command.subcommands.heuristics.RemoveCommand;
import de.photon.AACAdditionPro.command.subcommands.heuristics.SaveCommand;
import de.photon.AACAdditionPro.command.subcommands.heuristics.TrainCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Queue;

public class HeuristicsCommand extends InternalCommand
{
    public HeuristicsCommand()
    {
        super("heuristics", InternalPermission.NEURAL, (byte) 1,
              new CreateCommand(),
              new RemoveCommand(),
              new SaveCommand(),
              new TrainCommand());
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
    protected String[] getTabPossibilities()
    {
        return getChildTabs();
    }
}
