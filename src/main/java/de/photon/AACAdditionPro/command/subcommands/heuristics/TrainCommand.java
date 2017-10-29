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

public class TrainCommand extends InternalCommand
{
    public TrainCommand()
    {
        super("train", InternalPermission.NEURAL_TRAIN, (byte) 2);
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        if (AACAdditionPro.getInstance().getConfig().getBoolean("InventoryHeuristics.enabled"))
        {
            final Player p = Bukkit.getServer().getPlayer(arguments.poll());

            if (p == null)
            {
                sender.sendMessage(playerNotFoundMessage);
            }
            else
            {
                sender.sendMessage(prefix + ChatColor.GOLD + "[HEURISTICS] Training " + arguments.element().toUpperCase() + " Player: " + p.getName());
                VerboseSender.sendVerboseMessage("[HEURISTICS] Training " + arguments.element().toUpperCase() + "; Player: " + p.getName());
                AACAdditionPro.getInstance().getServer().getPluginManager().callEvent(new InventoryHeuristicsEvent(p, true, arguments.element().toUpperCase()));
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
        return new String[]{"Train the Inventory-Heuristics with an example-player"};
    }

    @Override
    protected String[] getTabPossibilities()
    {
        return getPlayerNameTabs();
    }
}
