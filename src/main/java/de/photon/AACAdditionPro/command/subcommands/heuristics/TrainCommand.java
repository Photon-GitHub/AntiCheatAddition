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

public class TrainCommand extends InternalCommand
{
    public TrainCommand()
    {
        super("train",
              (byte) 2,
              false,
              Permissions.NEURAL_TRAIN,
              "Train the Inventory-Heuristics with an example-player"
             );
    }

    @Override
    protected void execute(final CommandSender sender, final LinkedList<String> arguments)
    {
        final Player p = AACAdditionPro.getInstance().getServer().getPlayer(arguments.getFirst());

        if (p == null) {
            sender.sendMessage(playerNotFoundMessage);
        } else {
            if (arguments.get(1) == null || arguments.get(1).isEmpty()) {
                sender.sendMessage(prefix + ChatColor.GOLD + "You need to define a Pattern.");
            } else {
                sender.sendMessage(prefix + ChatColor.GOLD + "[HEURISTICS] Training " + arguments.get(1).toUpperCase() + " Player: " + p.getName());
                VerboseSender.sendVerboseMessage("[HEURISTICS] Training " + arguments.get(1).toUpperCase() + "; Player: " + p.getName());
                AACAdditionPro.getInstance().getServer().getPluginManager().callEvent(new InventoryHeuristicsEvent(p, true, arguments.get(1).toUpperCase()));
            }
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
