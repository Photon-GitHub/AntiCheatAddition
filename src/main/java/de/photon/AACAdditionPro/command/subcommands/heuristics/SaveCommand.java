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

public class SaveCommand extends InternalCommand
{
    public SaveCommand()
    {
        super("save", InternalPermission.NEURAL_CREATE, (byte) 2);
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        if (HeuristicsCommand.heurisitcsUnlocked())
        {
            final String patternName = arguments.remove();

            final Optional<Pattern> patternToDelete = InventoryHeuristics.getPATTERNS().stream().filter(pattern -> pattern.getName().equals(patternName)).findAny();

            if (patternToDelete.isPresent())
            {
                patternToDelete.get().saveToFile();

                sender.sendMessage(ChatColor.GOLD + "------" + ChatColor.DARK_RED + " Heuristics - Pattern " + ChatColor.GOLD + "------");
                sender.sendMessage(ChatColor.GOLD + "Saved pattern \"" + patternName + "\"");
            }
            else
            {
                sender.sendMessage(ChatColor.GOLD + "------" + ChatColor.DARK_RED + " Heuristics - Pattern " + ChatColor.GOLD + "------");
                sender.sendMessage(ChatColor.GOLD + "Pattern \"" + patternName + "\"" + " could not be found.");
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
        return new String[]{"Saves a pattern to a file that may be shared."};
    }

    @Override
    protected String[] getTabPossibilities()
    {
        return getChildTabs();
    }
}
