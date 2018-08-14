package de.photon.AACAdditionPro.command.subcommands;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.exceptions.NoViolationLevelManagementException;
import de.photon.AACAdditionPro.modules.ModuleType;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Queue;
import java.util.SortedMap;
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

        if (p == null)
        {
            InternalCommand.sendPlayerNotFoundMessage(sender);
        }
        else
        {
            final SortedMap<Integer, String> messages = new TreeMap<>(
                    (Integer o1, Integer o2) -> {
                        //The other way round
                        if (o1 > o2)
                        {
                            return -1;
                        }
                        if (o1.equals(o2))
                        {
                            return 0;
                        }
                        return 1;
                    });

            for (final ModuleType moduleType : ModuleType.values())
            {
                try
                {
                    // Casting is ok here as only AACAdditionProChecks will be in the CheckManager.
                    final int vl = AACAdditionPro.getInstance().getModuleManager().getViolationLevelManagement(moduleType).getVL(p.getUniqueId());
                    if (vl != 0)
                    {
                        messages.put(vl, AACAdditionPro.getInstance().getModuleManager().getModule(moduleType).getName());
                    }
                } catch (IllegalArgumentException | NoViolationLevelManagementException ignore)
                {
                    // Ignore the Exceptions as there are a few checks with no ViolatonLevelManagement or disabled checks.
                }
            }

            sender.sendMessage(PREFIX + ChatColor.GOLD + p.getName());

            if (messages.isEmpty())
            {
                sender.sendMessage(PREFIX + ChatColor.GOLD + "The player has no violations.");
            }
            else
            {
                messages.forEach((integer, s) -> sender.sendMessage(ChatColor.GOLD + s + " -> vl " + integer));
            }
        }
    }

    @Override
    protected String[] getCommandHelp()
    {
        return new String[]{"Display all violation levels of a player."};
    }

    @Override
    protected List<String> getTabPossibilities()
    {
        return getPlayerNameTabs();
    }
}
