package de.photon.anticheataddition.commands.subcommands;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.InternalPermission;
import de.photon.anticheataddition.commands.CommandAttributes;
import de.photon.anticheataddition.commands.InternalCommand;
import de.photon.anticheataddition.commands.TabCompleteSupplier;
import de.photon.anticheataddition.util.messaging.ChatMessage;
import org.bukkit.command.CommandSender;

import java.util.Queue;

/**
 * /anticheataddition reload
 * <p>Reloads the plugin's config.yml without needing a full /reload.</p>
 */
public final class ReloadCommand extends InternalCommand {

    public ReloadCommand() {
        super("reload",
              CommandAttributes.builder()
                               .setPermission(InternalPermission.INFO)
                               .exactArguments(0)
                               .setDescription("Reloads AntiCheatAddition's config.")
                               .build(),
              TabCompleteSupplier.builder());
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments) {
        AntiCheatAddition.getInstance().reloadConfig();
        ChatMessage.sendMessage(sender, "&aAntiCheatAddition configuration reloaded.");
    }
}
