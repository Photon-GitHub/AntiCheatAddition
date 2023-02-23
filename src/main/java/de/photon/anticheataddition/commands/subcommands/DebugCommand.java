package de.photon.anticheataddition.commands.subcommands;

import de.photon.anticheataddition.InternalPermission;
import de.photon.anticheataddition.commands.CommandAttributes;
import de.photon.anticheataddition.commands.InternalPlayerCommand;
import de.photon.anticheataddition.commands.TabCompleteSupplier;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.messaging.ChatMessage;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Queue;

public class DebugCommand extends InternalPlayerCommand
{
    private static final String ENABLED_MESSAGE = "Debug " + ChatColor.DARK_GREEN + "enabled";
    private static final String DISABLED_MESSAGE = "Debug " + ChatColor.RED + "disabled";


    public DebugCommand()
    {
        super("debug", CommandAttributes.builder()
                                        .minArguments(0)
                                        .maxArguments(1)
                                        .addCommandHelpLine("Used to toggle debug messages on and off.")
                                        .setPermission(InternalPermission.DEBUG)
                                        .build(), TabCompleteSupplier.builder().constants("on", "off"));
    }

    @Override
    protected void execute(Player sender, Queue<String> arguments)
    {
        final var user = User.getUser(sender.getUniqueId());
        if (user == null) return;

        final String nextArgument = arguments.peek();
        boolean toggleTo;
        if (nextArgument == null) toggleTo = !user.hasDebug();
        else toggleTo = switch (nextArgument.toLowerCase()) {
            case "on" -> true;
            case "off" -> false;
            default -> !user.hasDebug();
        };

        user.setDebug(toggleTo);
        ChatMessage.sendMessage(sender, toggleTo ? ENABLED_MESSAGE : DISABLED_MESSAGE);
    }
}
