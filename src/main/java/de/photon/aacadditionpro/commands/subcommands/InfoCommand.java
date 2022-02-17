package de.photon.aacadditionpro.commands.subcommands;

import de.photon.aacadditionpro.InternalPermission;
import de.photon.aacadditionpro.commands.CommandAttributes;
import de.photon.aacadditionpro.commands.InternalCommand;
import de.photon.aacadditionpro.commands.TabCompleteSupplier;
import de.photon.aacadditionpro.modules.ModuleManager;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.util.oldmessaging.ChatMessage;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.val;
import org.bukkit.command.CommandSender;

import java.util.Objects;
import java.util.Queue;
import java.util.stream.Collectors;

public class InfoCommand extends InternalCommand
{
    public InfoCommand()
    {
        super("info", CommandAttributes.builder().exactArguments(1)
                                       .addCommandHelp("Displays all violation levels of a player.",
                                                       "Syntax: /aacadditionpro info <player>")
                                       .setPermission(InternalPermission.INFO).build(), TabCompleteSupplier.builder().allPlayers());
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        // Peek for better performance
        val player = getPlayer(sender, arguments.peek());
        if (player == null) return;

        val moduleVls = ModuleManager.getViolationModuleMap().values().stream()
                                     // The of() method will return null when the vl is 0.
                                     .map(vlm -> ModuleVl.of(vlm, vlm.getManagement().getVL(player.getUniqueId())))
                                     .filter(Objects::nonNull)
                                     .sorted()
                                     .collect(Collectors.toList());

        ChatMessage.sendMessage(sender, player.getName());

        if (moduleVls.isEmpty()) ChatMessage.sendMessage(sender, "The player has no violations.");
        else moduleVls.forEach(moduleVl -> ChatMessage.sendMessage(sender, moduleVl.message));
    }

    @Value
    private static class ModuleVl implements Comparable<ModuleVl>
    {
        String message;
        @EqualsAndHashCode.Exclude int vl;

        public static ModuleVl of(ViolationModule vm, int vl)
        {
            // Return null when the vl is 0.
            return vl > 0 ? new ModuleVl(vm.getConfigString() + " -> vl " + vl, vl) : null;
        }

        @Override
        public int compareTo(ModuleVl o)
        {
            // Reversed to make the top vl appear first.
            return Integer.compare(o.vl, this.vl);
        }
    }
}
