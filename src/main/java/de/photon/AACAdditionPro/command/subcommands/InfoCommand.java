package de.photon.AACAdditionPro.command.subcommands;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.modules.ModuleType;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.SortedMap;
import java.util.TreeMap;

public class InfoCommand extends InternalCommand
{
    private SortedMap<Integer, ModuleType> messages = new TreeMap<>(Collections.reverseOrder(Integer::compareTo));

    public InfoCommand()
    {
        super("info",
              InternalPermission.INFO,
              (byte) 1);
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        // Peek for better performance
        final Player player = AACAdditionPro.getInstance().getServer().getPlayer(arguments.peek());

        if (player == null)
        {
            sendPlayerNotFoundMessage(sender);
            return;
        }

        for (ModuleType moduleType : ModuleType.VL_MODULETYPES)
        {
            // Casting is ok here as only AACAdditionProChecks will be in the CheckManager.
            final int vl = AACAdditionPro.getInstance().getModuleManager().getViolationLevelManagement(moduleType).getVL(player.getUniqueId());
            if (vl != 0)
            {
                messages.put(vl, moduleType);
            }
        }

        sender.sendMessage(PREFIX + ChatColor.GOLD + player.getName());

        if (messages.isEmpty())
        {
            sender.sendMessage(PREFIX + ChatColor.GOLD + "The player has no violations.");
        }
        else
        {
            messages.forEach((integer, moduleType) -> sender.sendMessage(ChatColor.GOLD + moduleType.getConfigString() + " -> vl " + integer));
        }

        messages.clear();
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
