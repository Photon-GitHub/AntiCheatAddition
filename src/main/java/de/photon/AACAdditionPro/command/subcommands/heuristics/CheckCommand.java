package de.photon.AACAdditionPro.command.subcommands.heuristics;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.Permissions;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.events.InventoryHeuristicsEvent;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CheckCommand extends InternalCommand
{
    public CheckCommand()
    {
        super("check", (byte) 1, false, Permissions.NEURAL_CHECK, "Manually check a player with the heuristics");
    }

    @Override
    protected void execute(final CommandSender sender, final LinkedList<String> arguments)
    {
        final Player p = AACAdditionPro.getInstance().getServer().getPlayer(arguments.getFirst());

        if (p == null) {
            sender.sendMessage(playerNotFoundMessage);
        } else {
            sender.sendMessage(prefix + ChatColor.GOLD + "[HEURISTICS] Manual check of " + p.getName());
            VerboseSender.sendVerboseMessage("[HEURISTICS] Manual check of " + p.getName());
            AACAdditionPro.getInstance().getServer().getPluginManager().callEvent(new InventoryHeuristicsEvent(p, false, ""));
        }
    }

    @Override
    protected List<String> getTabPossibilities()
    {
        final ArrayList<String> tab = new ArrayList<>();
        for (final Player p : AACAdditionPro.getInstance().getServer().getOnlinePlayers()) {
            tab.add(p.getName());
        }
        return tab;
    }
}
