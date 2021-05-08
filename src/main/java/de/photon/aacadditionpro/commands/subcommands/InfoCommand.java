package de.photon.aacadditionpro.commands.subcommands;

import de.photon.aacadditionpro.InternalPermission;
import de.photon.aacadditionpro.commands.CommandAttributes;
import de.photon.aacadditionpro.commands.InternalCommand;
import de.photon.aacadditionpro.commands.TabCompleteSupplier;
import de.photon.aacadditionpro.modules.ModuleManager;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.util.messaging.ChatMessage;
import lombok.EqualsAndHashCode;
import lombok.val;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Queue;

public class InfoCommand extends InternalCommand
{
    public InfoCommand()
    {
        super("info", CommandAttributes.builder().exactArguments(1)
                                       .addCommandHelpLine("Displays all violation levels of a player.")
                                       .setPermission(InternalPermission.INFO).build(), TabCompleteSupplier.builder().allPlayers());
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        // Peek for better performance
        val player = getPlayer(sender, arguments.peek());
        if (player == null) return;

        val moduleVls = new ArrayList<ModuleVl>();
        int vl;
        for (ViolationModule vm : ModuleManager.getViolationModuleMap().values()) {
            vl = vm.getManagement().getVL(player.getUniqueId());
            if (vl > 0) moduleVls.add(new ModuleVl(vm, vl));
        }

        ChatMessage.sendMessage(sender, player.getName());

        if (moduleVls.isEmpty()) {
            ChatMessage.sendMessage(sender, "The player has no violations.");
        } else {
            moduleVls.sort(ModuleVl::compareTo);
            moduleVls.forEach(moduleVl -> ChatMessage.sendMessage(sender, moduleVl.message));
        }
    }

    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    private static class ModuleVl implements Comparable<ModuleVl>
    {
        @EqualsAndHashCode.Include private final String message;
        private final int vl;

        public ModuleVl(ViolationModule vm, int vl)
        {
            this.message = vm.getConfigString() + " -> vl " + vl;
            this.vl = vl;
        }

        @Override
        public int compareTo(ModuleVl o)
        {
            // Reversed to make the top vl appear first.
            return Integer.compare(o.vl, this.vl);
        }
    }
}
