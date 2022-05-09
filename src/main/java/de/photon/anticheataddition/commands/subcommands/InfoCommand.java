package de.photon.anticheataddition.commands.subcommands;

import de.photon.anticheataddition.InternalPermission;
import de.photon.anticheataddition.commands.CommandAttributes;
import de.photon.anticheataddition.commands.InternalCommand;
import de.photon.anticheataddition.commands.TabCompleteSupplier;
import de.photon.anticheataddition.modules.ModuleManager;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.util.messaging.ChatMessage;
import lombok.EqualsAndHashCode;
import lombok.val;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Queue;

public class InfoCommand extends InternalCommand
{
    public InfoCommand()
    {
        super("info", CommandAttributes.builder().exactArguments(1)
                                       .addCommandHelp("Displays all violation levels of a player.",
                                                       "Syntax: /anticheataddition info <player>")
                                       .setPermission(InternalPermission.INFO).build(), TabCompleteSupplier.builder().allPlayers());
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        // Peek for better performance
        val player = getPlayer(sender, arguments.peek());
        if (player == null) return;

        val moduleVls = ModuleManager.getViolationModuleMap().values().stream()
                                     .map(vlm -> new ModuleVl(vlm, player))
                                     // Only the modules with above 0 vl are important.
                                     .filter(mvl -> mvl.vl > 0)
                                     .sorted()
                                     .toList();

        ChatMessage.sendMessage(sender, player.getName());

        if (moduleVls.isEmpty()) ChatMessage.sendMessage(sender, "The player has no violations.");
        else moduleVls.forEach(moduleVl -> ChatMessage.sendMessage(sender, moduleVl.message));
    }

    @EqualsAndHashCode
    private static class ModuleVl implements Comparable<ModuleVl>
    {
        @EqualsAndHashCode.Exclude private final int vl;
        private final String message;

        private ModuleVl(ViolationModule vm, Player player)
        {
            this.vl = vm.getManagement().getVL(player.getUniqueId());
            this.message = vm.getConfigString() + " -> vl " + vl;
        }

        @Override
        public int compareTo(ModuleVl o)
        {
            // Reversed to make the top vl appear first.
            return Integer.compare(o.vl, this.vl);
        }
    }
}
