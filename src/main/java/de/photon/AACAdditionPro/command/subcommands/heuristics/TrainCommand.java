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

import java.util.Collection;
import java.util.Queue;
import java.util.Set;

public class TrainCommand extends InternalCommand
{
    public TrainCommand()
    {
        super("train", InternalPermission.NEURAL_TRAIN, (byte) 2);
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        if (AACAdditionPro.getInstance().getConfig().getBoolean("InventoryHeuristics.enabled")) {
            final Player p = AACAdditionPro.getInstance().getServer().getPlayer(arguments.poll());

            if (p == null) {
                sender.sendMessage(playerNotFoundMessage);
            } else {
                sender.sendMessage(prefix + ChatColor.GOLD + "[HEURISTICS] Training " + arguments.element().toUpperCase() + " Player: " + p.getName());
                VerboseSender.sendVerboseMessage("[HEURISTICS] Training " + arguments.element().toUpperCase() + "; Player: " + p.getName());
                AACAdditionPro.getInstance().getServer().getPluginManager().callEvent(new InventoryHeuristicsEvent(p, true, arguments.element().toUpperCase()));
            }
        }
    }

    @Override
    protected String[] getCommandHelp()
    {
        return new String[]{"Train the Inventory-Heuristics with an example-player"};
    }

    @Override
    protected Set<InternalCommand> getChildCommands()
    {
        return null;
    }

    @Override
    protected String[] getTabPossibilities()
    {
        final Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        final String[] tab = new String[onlinePlayers.size()];
        int index = 0;
        for (Player player : onlinePlayers) {
            tab[index++] = player.getName();
        }
        return tab;
    }
}
