package de.photon.anticheataddition.commands.subcommands;

import de.photon.anticheataddition.InternalPermission;
import de.photon.anticheataddition.commands.CommandAttributes;
import de.photon.anticheataddition.commands.InternalCommand;
import de.photon.anticheataddition.commands.TabCompleteSupplier;
import de.photon.anticheataddition.commands.subcommands.internaltestcommands.TestInventoryClick;
import de.photon.anticheataddition.commands.subcommands.internaltestcommands.TestInventoryOpen;
import de.photon.anticheataddition.util.messaging.ChatMessage;
import org.bukkit.command.CommandSender;

import java.util.Queue;

public class InternalTestCommand extends InternalCommand
{
    public InternalTestCommand()
    {
        super("internaltest", CommandAttributes.builder()
                                               .addCommandHelp("Allows to test AntiCheatAddition's internal mechanics.",
                                                               "This is helpful to trigger different situations without installing a cheat client.",
                                                               "Syntax: /anticheataddition internaltest <subcommand>")
                                               .addChildCommands(new TestInventoryClick(),
                                                                 new TestInventoryOpen())
                                               .setPermission(InternalPermission.INTERNALTEST).build(), TabCompleteSupplier.builder());
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        ChatMessage.sendMessage(sender, "AntiCheatAddition's internal testing framework. Use the subcommands to test different situations.");
    }
}
