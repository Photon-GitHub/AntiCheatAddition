package de.photon.AACAdditionPro.command.subcommands;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.Module;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.checks.CheckManager;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.exceptions.NoViolationLevelManagementException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

public class InfoCommand extends InternalCommand
{
    public InfoCommand()
    {
        super("info", InternalPermission.INFO, (byte) 1);
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        final Player p = AACAdditionPro.getInstance().getServer().getPlayer(arguments.peek());

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
                try {
                    // Casting is ok here as only AACAdditionProChecks will be in the CheckManager.
                    Integer vl = ((AACAdditionProCheck) module).getViolationLevelManagement().getVL(p.getUniqueId());
                    if (vl != 0) {
                        messages.put(vl, module.getName());
                    }
                } catch (NoViolationLevelManagementException ignore) {
                    // Ignore the Exception as there are a few checks with no ViolatonLevelManagement
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
    protected String[] getCommandHelp()
    {
        return new String[]{"Display all Violation-Levels of a player."};
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
