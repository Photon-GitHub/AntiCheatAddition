package de.photon.anticheataddition.commands.subcommands.internaltestcommands;

import de.photon.anticheataddition.InternalPermission;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.commands.CommandAttributes;
import de.photon.anticheataddition.commands.InternalCommand;
import de.photon.anticheataddition.commands.TabCompleteSupplier;
import de.photon.anticheataddition.user.data.DataUpdaterEvents;
import de.photon.anticheataddition.util.inventoryview.InventoryViewUtil;
import de.photon.anticheataddition.util.messaging.ChatMessage;
import de.photon.anticheataddition.util.log.Log;
import org.bukkit.command.CommandSender;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

import java.util.Queue;
import java.util.Random;

public class TestInventoryClick extends InternalCommand
{
    private static final Random RANDOM = new Random();


    public TestInventoryClick()
    {
        super("inventoryclick", CommandAttributes.builder()
                                                 .addCommandHelp("This command sets the internal state of a player as if they had clicked a random slot in a chest inventory.",
                                                                 "Syntax: /anticheataddition internaltest inventoryclick <player> [amount of clicks]")
                                                 .minArguments(1)
                                                 .maxArguments(2)
                                                 .setPermission(InternalPermission.INTERNALTEST).build(), TabCompleteSupplier.builder().allPlayers());
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        final var user = parseUser(sender, arguments.poll());
        if (user == null) return;

        if (ServerVersion.ACTIVE == ServerVersion.MC120) {
            ChatMessage.sendMessage(sender, "Due to various API changes this command is not available on Minecraft 1.20.");
            return;
        }

        int count = 1;

        if (!arguments.isEmpty()) {
            final var parsedCount = parseIntElseSend(sender, arguments.poll());
            if (parsedCount != null) count = parsedCount;
        }

        final var view = InventoryViewUtil.INSTANCE.createTestView(user);

        for (int i = 0; i < count; ++i) {
            DataUpdaterEvents.INSTANCE.onInventoryClick(new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, RANDOM.nextInt(27), ClickType.LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY));
        }

        Log.fine(() -> "Executed internal inventory click test command affecting player " + user.getPlayer().getName() + " requested by " + sender.getName());
        ChatMessage.sendMessage(sender, count + " internal clicks for player " + user.getPlayer().getName() + " executed.");
    }
}
