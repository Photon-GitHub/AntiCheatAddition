package de.photon.AACAdditionPro.command.subcommands.heuristics;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.events.InventoryHeuristicsEvent;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Queue;
import java.util.Set;

public class CheckCommand extends InternalCommand
{
    public CheckCommand()
    {
        super("check", InternalPermission.NEURAL_CHECK, (byte) 1);
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        if (AACAdditionPro.getInstance().getConfig().getBoolean("InventoryHeuristics.enabled"))
        {
            final Player p = Bukkit.getServer().getPlayer(arguments.peek());

            if (p == null)
            {
                sender.sendMessage(playerNotFoundMessage);
            }
            else
            {
                sender.sendMessage(prefix + ChatColor.GOLD + "[HEURISTICS] Manual check of " + p.getName());
                VerboseSender.sendVerboseMessage("[HEURISTICS] Manual check of " + p.getName());
                AACAdditionPro.getInstance().getServer().getPluginManager().callEvent(new InventoryHeuristicsEvent(p, false, ""));
            }
        }
        else
        {
            sender.sendMessage(prefix + ChatColor.RED + "InventoryHeuristics is not loaded / enabled.");
        }
    }

    @Override
    protected String[] getCommandHelp()
    {
        return new String[]{"Manually check a player with the heuristics"};
    }

    @Override
    protected Set<InternalCommand> getChildCommands()
    {
        return null;
    }

    @Override
    protected String[] getTabPossibilities()
    {
        return getPlayerNameTabs();
    }
}
