package de.photon.AACAdditionPro.command;

import de.photon.AACAdditionPro.InternalPermission;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Queue;

public abstract class InternalPlayerCommand extends InternalCommand
{
    public InternalPlayerCommand(String name, InternalPermission permission, InternalCommand... childCommands)
    {
        super(name, permission, childCommands);
    }

    public InternalPlayerCommand(String name, InternalPermission permission, byte minArguments, InternalCommand... childCommands)
    {
        super(name, permission, minArguments, childCommands);
    }

    public InternalPlayerCommand(String name, InternalPermission permission, byte minArguments, byte maxArguments, InternalCommand... childCommands)
    {
        super(name, permission, minArguments, maxArguments, childCommands);
    }

    @Override
    void invokeCommand(CommandSender sender, Queue<String> arguments)
    {
        // Only players
        if (!(sender instanceof Player))
        {
            sender.sendMessage(PREFIX + ChatColor.RED + "Only a player can use this command.");
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
