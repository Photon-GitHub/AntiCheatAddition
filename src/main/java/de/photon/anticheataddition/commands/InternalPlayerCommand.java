package de.photon.anticheataddition.commands;

import de.photon.anticheataddition.util.messaging.ChatMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Queue;

public abstract class InternalPlayerCommand extends InternalCommand
{
    protected InternalPlayerCommand(String name, CommandAttributes commandAttributes, TabCompleteSupplier.Builder tabCompleteSupplier)
    {
        super(name, commandAttributes, tabCompleteSupplier);
    }

    @Override
    protected void invokeCommand(@NotNull CommandSender sender, @NotNull Queue<String> arguments)
    {
//        // Only players
//        if (!(sender instanceof Player)) {
//            ChatMessage.sendMessage(sender, "Only a player can use this command.");
//            return;
//        }

        //Applied Pull-up method by moving player-sender validation to InternalCommand.ensurePlayerSender(). Simplified InternalPlayerCommand.invokeCommand, enhancing modularity and reusability.
        if (!ensurePlayerSender(sender)) return;
        super.invokeCommand(sender, arguments);
    }

    @Override
    protected final void execute(CommandSender sender, Queue<String> arguments)
    {
        this.execute((Player) sender, arguments);
    }

    protected abstract void execute(Player sender, Queue<String> arguments);
}
