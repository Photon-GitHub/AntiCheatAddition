package de.photon.AACAdditionPro.command.subcommands.heuristics;

import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.checks.subchecks.InventoryHeuristics;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.command.subcommands.HeuristicsCommand;
import de.photon.AACAdditionPro.heuristics.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Queue;

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

            // The Heuristics Header will always be sent.
            sender.sendMessage(HeuristicsCommand.HEURISTICS_HEADER);
            sender.sendMessage(InventoryHeuristics.PATTERNS.remove(patternName) == null ?
                               HeuristicsCommand.createPatternNotFoundMessage(patternName) :
                               ChatColor.GOLD + "Deleted pattern \"" + ChatColor.RED + patternName + ChatColor.GOLD + "\".");
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
        return InventoryHeuristics.PATTERNS.values().stream().map(Pattern::getName).toArray(String[]::new);
    }
}
