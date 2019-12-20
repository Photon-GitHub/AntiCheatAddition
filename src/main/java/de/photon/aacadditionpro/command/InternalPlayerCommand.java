package de.photon.aacadditionpro.command;

import de.photon.aacadditionpro.InternalPermission;
import de.photon.aacadditionpro.util.messaging.ChatMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Queue;
import java.util.Set;

public abstract class InternalPlayerCommand extends InternalCommand
{
    public InternalPlayerCommand(String name, InternalPermission permission, List<String> commandHelp, Set<InternalCommand> childCommands, TabCompleteSupplier.Builder tabCompleteSupplier)
    {
        super(name, permission, commandHelp, childCommands, tabCompleteSupplier);
    }

    public InternalPlayerCommand(String name, InternalPermission permission, int minArguments, List<String> commandHelp, Set<InternalCommand> childCommands, TabCompleteSupplier.Builder tabCompleteSupplier)
    {
        super(name, permission, minArguments, commandHelp, childCommands, tabCompleteSupplier);
    }

    public InternalPlayerCommand(String name, InternalPermission permission, int minArguments, int maxArguments, List<String> commandHelp, Set<InternalCommand> childCommands, TabCompleteSupplier.Builder tabCompleteSupplier)
    {
        super(name, permission, minArguments, maxArguments, commandHelp, childCommands, tabCompleteSupplier);
    }

    @Override
    void invokeCommand(CommandSender sender, Queue<String> arguments)
    {
        // Only players
        if (!(sender instanceof Player)) {
            ChatMessage.sendErrorMessage(sender, "Only a player can use this command.");
            return;
        }

        super.invokeCommand(sender, arguments);
    }

    @Override
    protected final void execute(CommandSender sender, Queue<String> arguments)
    {
        this.execute((Player) sender, arguments);
    }

    protected abstract void execute(Player sender, Queue<String> arguments);
}
