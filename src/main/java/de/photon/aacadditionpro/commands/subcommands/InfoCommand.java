package de.photon.aacadditionpro.commands.subcommands;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.InternalPermission;
import de.photon.aacadditionpro.commands.CommandAttributes;
import de.photon.aacadditionpro.commands.InternalCommand;
import de.photon.aacadditionpro.commands.TabCompleteSupplier;
import de.photon.aacadditionpro.util.messaging.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
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
        super("info", CommandAttributes.builder().exactArguments(1)
                                       .addCommandHelpLine("Displays all violation levels of a player.")
                                       .setPermission(InternalPermission.INFO).build(), TabCompleteSupplier.builder().allPlayers().commandHelp());
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        // Peek for better performance
        final Player player = AACAdditionPro.getInstance().getServer().getPlayer(arguments.peek());

        if (player == null) {
            ChatMessage.sendPlayerNotFoundMessage(sender);
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

        ChatMessage.sendInfoMessage(sender, ChatColor.GOLD, player.getName());

        if (messages.isEmpty()) {
            ChatMessage.sendInfoMessage(sender, ChatColor.GOLD, "The player has no violations.");
        } else {
            messages.sort(ModuleVl::compareTo);
            messages.forEach(moduleVl -> ChatMessage.sendInfoMessage(sender, ChatColor.GOLD, moduleVl.getDisplayMessage()));
        }
    }

    @AllArgsConstructor
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    private static class ModuleVl implements Comparable<ModuleVl>
    {
        @EqualsAndHashCode.Include private final ModuleType moduleType;
        private final int vl;

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
