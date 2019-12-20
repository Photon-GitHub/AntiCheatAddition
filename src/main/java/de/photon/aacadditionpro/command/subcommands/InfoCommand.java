package de.photon.aacadditionpro.command.subcommands;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.InternalPermission;
import de.photon.aacadditionpro.command.InternalCommand;
import de.photon.aacadditionpro.modules.ModuleType;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class InfoCommand extends InternalCommand
{
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

        if (player == null) {
            sendPlayerNotFoundMessage(sender);
            return;
        }

        final List<ModuleVl> messages = new ArrayList<>();
        int vl;
        for (ModuleType moduleType : ModuleType.VL_MODULETYPES) {
            vl = AACAdditionPro.getInstance().getModuleManager().getViolationLevelManagement(moduleType).getVL(player.getUniqueId());
            if (vl != 0) {
                messages.add(new ModuleVl(moduleType, vl));
            }
        }

        sender.sendMessage(PREFIX + ChatColor.GOLD + player.getName());

        if (messages.isEmpty()) {
            sender.sendMessage(PREFIX + ChatColor.GOLD + "The player has no violations.");
        }
        else {
            messages.sort(ModuleVl::compareTo);
            messages.forEach((moduleVl) -> sender.sendMessage(ChatColor.GOLD + moduleVl.getDisplayMessage()));
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

    private static class ModuleVl implements Comparable<ModuleVl>
    {
        private final ModuleType moduleType;
        private final int vl;

        public ModuleVl(ModuleType moduleType, int vl)
        {
            this.moduleType = moduleType;
            this.vl = vl;
        }

        public String getDisplayMessage()
        {
            return this.moduleType.getConfigString() + " -> vl " + this.vl;
        }

        @Override
        public int compareTo(ModuleVl o)
        {
            // Reversed to make the top vl appear first.
            return Integer.compare(o.vl, this.vl);
        }
    }
}
