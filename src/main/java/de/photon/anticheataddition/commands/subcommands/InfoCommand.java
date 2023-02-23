package de.photon.anticheataddition.commands.subcommands;

import de.photon.anticheataddition.InternalPermission;
import de.photon.anticheataddition.commands.CommandAttributes;
import de.photon.anticheataddition.commands.InternalCommand;
import de.photon.anticheataddition.commands.TabCompleteSupplier;
import de.photon.anticheataddition.modules.ModuleManager;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.util.messaging.ChatMessage;
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
        final var player = getPlayer(sender, arguments.peek());
        if (player == null) return;

        final var moduleVls = ModuleManager.getViolationModuleMap().values().stream()
                                           .map(vlm -> ModuleVl.of(vlm, player))
                                           // Only the modules with above 0 vl are important.
                                           .filter(ModuleVl::hasValidVl)
                                           .sorted()
                                           .toList();

        ChatMessage.sendMessage(sender, player.getName());

        if (moduleVls.isEmpty()) ChatMessage.sendMessage(sender, "The player has no violations.");
        else moduleVls.forEach(moduleVl -> ChatMessage.sendMessage(sender, moduleVl.message));
    }

    private record ModuleVl(int vl, String message) implements Comparable<ModuleVl>
    {
        private static ModuleVl of(ViolationModule vm, Player player)
        {
            final int vl = vm.getManagement().getVL(player.getUniqueId());
            return new ModuleVl(vl, vm.getConfigString() + " -> vl " + vl);
        }

        public boolean hasValidVl()
        {
            return this.vl > 0;
        }

        @Override
        public int compareTo(ModuleVl o)
        {
            // Reversed to make the top vl appear first.
            return Integer.compare(o.vl, this.vl);
        }
    }
}
