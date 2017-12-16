package de.photon.AACAdditionPro.command.subcommands.heuristics;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.checks.subchecks.InventoryHeuristics;
import de.photon.AACAdditionPro.command.InternalCommand;
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
            sender.sendMessage(ChatColor.GOLD + "------" + ChatColor.DARK_RED + " Heuristics - Pattern " + ChatColor.GOLD + "------");

            final StringBuilder messageBuilder = new StringBuilder(64);
            messageBuilder.append(ChatColor.RED);
            messageBuilder.append("Active patterns: \n");

            int patterns = 0;
            for (Pattern pattern : InventoryHeuristics.getPATTERNS())
            {
                messageBuilder.append(pattern.getName());
                messageBuilder.append(" ,");

                if (++patterns > 4)
                {
                    messageBuilder.append("\n");
                    messageBuilder.append(ChatColor.GOLD);
                    patterns = 0;
                }
            }

            sender.sendMessage(messageBuilder.toString());
        }
        else
        {
            sender.sendMessage(prefix + ChatColor.RED + "InventoryHeuristics framework is not loaded or enabled.");
        }
    }

    @Override
    protected String[] getCommandHelp()
    {
        return new String[]{"Lists all active patterns."};
    }

    @Override
    protected String[] getTabPossibilities()
    {
        return getChildTabs();
    }
}
