package de.photon.anticheataddition.commands.subcommands;

import de.photon.anticheataddition.InternalPermission;
import de.photon.anticheataddition.commands.CommandAttributes;
import de.photon.anticheataddition.commands.InternalCommand;
import de.photon.anticheataddition.commands.TabCompleteSupplier;
import de.photon.anticheataddition.commands.subcommands.internaltestcommands.TestInventoryClick;
import de.photon.anticheataddition.commands.subcommands.internaltestcommands.TestInventoryOpen;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.messaging.ChatMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

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

    public static InventoryView createInventoryView(User user)
    {
        final Inventory top = Bukkit.createInventory(user.getPlayer(), InventoryType.CHEST);
        final Inventory bottom = user.getPlayer().getInventory();

        // Fill the top inventory with stone.
        top.addItem(new ItemStack(Material.STONE, 1700));

        return new InventoryView()
        {
            @Override
            public @NotNull Inventory getTopInventory()
            {
                return top;
            }

            @Override
            public @NotNull Inventory getBottomInventory()
            {
                return bottom;
            }

            @Override
            public @NotNull HumanEntity getPlayer()
            {
                return user.getPlayer();
            }

            @Override
            public @NotNull InventoryType getType()
            {
                return InventoryType.CHEST;
            }

            @Override
            public @NotNull String getTitle()
            {
                return "TestInventory";
            }
        };
    }
}
