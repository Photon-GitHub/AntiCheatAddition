package de.photon.AACAdditionPro.command.subcommands.heuristics;

import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.checks.subchecks.InventoryHeuristics;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.command.subcommands.HeuristicsCommand;
import de.photon.AACAdditionPro.heuristics.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

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
            final Pattern patternToRename = InventoryHeuristics.getPatternByName(patternName);

            // The Heuristics Header will always be sent.
            sender.sendMessage(HeuristicsCommand.HEURISTICS_HEADER);

            if (patternToRename == null)
            {
                sender.sendMessage(HeuristicsCommand.createPatternNotFoundMessage(patternName));
            }
            else
            {
                final String newName = arguments.remove();

                if (InventoryHeuristics.getPATTERNS().stream().anyMatch(pattern -> pattern.getName().equals(newName)))
                {
                    sender.sendMessage(ChatColor.GOLD + "Cannot rename the pattern to " + ChatColor.RED + newName + ChatColor.GOLD + " as another pattern with the same name exists.");
                }
                else
                {
                    patternToRename.setName(newName);
                    sender.sendMessage(ChatColor.GOLD + "Renamed pattern \"" + ChatColor.RED + patternName + ChatColor.GOLD + "\" to " + ChatColor.RED + newName);
                }
            }
        }
        else
        {
            sender.sendMessage(HeuristicsCommand.FRAMEWORK_DISABLED);
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
