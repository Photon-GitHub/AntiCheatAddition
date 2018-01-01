package de.photon.AACAdditionPro.command.subcommands.heuristics;

import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.checks.subchecks.InventoryHeuristics;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.command.subcommands.HeuristicsCommand;
import de.photon.AACAdditionPro.heuristics.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Queue;

public class SaveCommand extends InternalCommand
{
    public SaveCommand()
    {
        super("save", InternalPermission.NEURAL_SAVE, (byte) 1);
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

            if (patternToDelete == null)
            {
                sender.sendMessage(HeuristicsCommand.createPatternNotFoundMessage(patternName));
            }
            else
            {
                patternToDelete.saveToFile();
                sender.sendMessage(ChatColor.GOLD + "Saved pattern \"" + ChatColor.RED + patternName + ChatColor.GOLD + "\"");
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
        return new String[]{"Saves a pattern to a file that may be shared."};
    }

    @Override
    protected String[] getTabPossibilities()
    {
        return getChildTabs();
    }
}
