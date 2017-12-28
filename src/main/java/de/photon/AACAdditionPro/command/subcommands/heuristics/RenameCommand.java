package de.photon.AACAdditionPro.command.subcommands.heuristics;

import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.checks.subchecks.InventoryHeuristics;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.command.subcommands.HeuristicsCommand;
import de.photon.AACAdditionPro.heuristics.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Optional;
import java.util.Queue;

public class RenameCommand extends InternalCommand
{
    public RenameCommand()
    {
        super("rename", InternalPermission.NEURAL_CREATE, (byte) 1);
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        if (HeuristicsCommand.heuristicsUnlocked())
        {
            final String patternName = arguments.remove();

            final Optional<Pattern> patternToRename = InventoryHeuristics.getPATTERNS().stream().filter(pattern -> pattern.getName().equals(patternName)).findAny();

            if (patternToRename.isPresent())
            {
                final String newName = arguments.remove();
                patternToRename.get().setName(newName);

                sender.sendMessage(HeuristicsCommand.HEURISTICS_HEADER);
                sender.sendMessage(ChatColor.GOLD + "Renamed pattern \"" + ChatColor.RED + patternName + ChatColor.GOLD + "\" to " + ChatColor.RED + newName);
            }
            else
            {
                sender.sendMessage(HeuristicsCommand.HEURISTICS_HEADER);
                sender.sendMessage(ChatColor.GOLD + "Pattern \"" + ChatColor.RED + patternName + ChatColor.GOLD + "\"" + " could not be found.");
            }
        }
        else
        {
            sender.sendMessage(prefix + ChatColor.RED + "InventoryHeuristics framework is not loaded, enabled or unlocked.");
        }
    }

    @Override
    protected String[] getCommandHelp()
    {
        return new String[]{"Renames a pattern internally. Saving the pattern afterwards is recommended."};
    }

    @Override
    protected String[] getTabPossibilities()
    {
        return getChildTabs();
    }
}
