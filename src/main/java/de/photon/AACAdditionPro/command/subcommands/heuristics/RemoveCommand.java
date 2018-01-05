package de.photon.AACAdditionPro.command.subcommands.heuristics;

import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.checks.subchecks.InventoryHeuristics;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.command.subcommands.HeuristicsCommand;
import de.photon.AACAdditionPro.heuristics.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Queue;
import java.util.Set;

public class RemoveCommand extends InternalCommand
{
    public RemoveCommand()
    {
        super("remove", InternalPermission.NEURAL_CREATE, (byte) 1);
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        if (HeuristicsCommand.heuristicsUnlocked())
        {
            final String patternName = arguments.remove();
            final Pattern patternToDelete = InventoryHeuristics.getPatternByName(patternName);

            // The Heuristics Header will always be sent.
            sender.sendMessage(HeuristicsCommand.HEURISTICS_HEADER);

            if (patternToDelete != null)
            {
                InventoryHeuristics.getPATTERNS().remove(patternToDelete);
                sender.sendMessage(ChatColor.GOLD + "Deleted pattern \"" + ChatColor.RED + patternName + ChatColor.GOLD + "\"");
            }
            else
            {
                sender.sendMessage(HeuristicsCommand.createPatternNotFoundMessage(patternName));
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
        return new String[]{"Removes a pattern from the loaded pattern list. Does not remove the file of a pattern."};
    }

    @Override
    protected String[] getTabPossibilities()
    {
        final Set<Pattern> currentPatterns = InventoryHeuristics.getPATTERNS();
        String[] patternNames = new String[currentPatterns.size()];

        int index = 0;
        for (Pattern currentPattern : currentPatterns)
        {
            patternNames[index++] = currentPattern.getName();
        }

        return patternNames;
    }
}
