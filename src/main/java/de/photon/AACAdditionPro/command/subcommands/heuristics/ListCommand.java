package de.photon.AACAdditionPro.command.subcommands.heuristics;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.checks.subchecks.InventoryHeuristics;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.command.subcommands.HeuristicsCommand;
import de.photon.AACAdditionPro.heuristics.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Queue;

public class ListCommand extends InternalCommand
{
    public ListCommand()
    {
        super("list", InternalPermission.NEURAL, (byte) 0);
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        if (AACAdditionPro.getInstance().getConfig().getBoolean("InventoryHeuristics.enabled"))
        {
            sender.sendMessage(HeuristicsCommand.HEURISTICS_HEADER);

            if (!InventoryHeuristics.getPATTERNS().isEmpty())
            {
                final StringBuilder messageBuilder = new StringBuilder(64);
                messageBuilder.append(ChatColor.RED);
                messageBuilder.append("Active heuristics: \n");
                messageBuilder.append(ChatColor.GOLD);

                int patterns = 0;
                for (Pattern pattern : InventoryHeuristics.getPATTERNS())
                {
                    if (++patterns > 5)
                    {
                        messageBuilder.append("\n");
                        messageBuilder.append(ChatColor.GOLD);
                        patterns = 0;
                    }

                    messageBuilder.append(pattern.getName());
                    messageBuilder.append(", ");
                }

                // Delete the last comma and space
                for (int i = 0; i < 2; i++)
                {
                    messageBuilder.deleteCharAt(messageBuilder.length() - 1);
                }

                sender.sendMessage(messageBuilder.toString());
            }
            else
            {
                sender.sendMessage(ChatColor.RED + "No active heuristics found.");
            }
        }
        else
        {
            sender.sendMessage(PREFIX + ChatColor.RED + "InventoryHeuristics framework is not loaded or enabled.");
        }
    }

    @Override
    protected String[] getCommandHelp()
    {
        return new String[]{"Lists all active heuristics."};
    }

    @Override
    protected String[] getTabPossibilities()
    {
        return getChildTabs();
    }
}
