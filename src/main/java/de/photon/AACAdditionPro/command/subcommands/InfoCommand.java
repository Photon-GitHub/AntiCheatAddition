package de.photon.AACAdditionPro.command.subcommands;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.Module;
import de.photon.AACAdditionPro.Permissions;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.checks.CheckManager;
import de.photon.AACAdditionPro.command.InternalCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

public class InfoCommand extends InternalCommand
{
    public InfoCommand()
    {
        super("info", (byte) 1, false, Permissions.INFO, "Display all Violation-Levels of a player.");
    }

    @Override
    protected void execute(final CommandSender sender, final LinkedList<String> arguments)
    {
        final Player p = AACAdditionPro.getInstance().getServer().getPlayer(arguments.getFirst());

        if (p == null) {
            sender.sendMessage(playerNotFoundMessage);
        } else {
            final TreeMap<Integer, String> messages = new TreeMap<>(
                    (Integer o1, Integer o2) -> {
                        //The other way round
                        if (o1 > o2) {
                            return -1;
                        }
                        if (o1.equals(o2)) {
                            return 0;
                        }
                        return 1;
                    });

            for (final Module module : CheckManager.checkManagerInstance.getManagedObjects()) {
                // Casting is ok here as only AACAdditionProChecks will be in the CheckManager.
                final AACAdditionProCheck check = (AACAdditionProCheck) module;
                if (check.hasViolationLevelManagement()) {
                    final int vl = check.getViolationLevelManagement().getVL(p.getUniqueId());
                    if (vl > 0) {
                        messages.put(vl, check.getName());
                    }
                }
            }

            sender.sendMessage(prefix + ChatColor.GOLD + p.getName());

            if (messages.isEmpty()) {
                sender.sendMessage(prefix + ChatColor.GOLD + "The player has no violations.");
            } else {
                messages.forEach((integer, s) -> sender.sendMessage(ChatColor.GOLD + s + " -> vl " + integer));
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
