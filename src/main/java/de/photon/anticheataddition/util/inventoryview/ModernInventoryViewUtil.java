package de.photon.anticheataddition.util.inventoryview;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ModernInventoryViewUtil implements InventoryViewUtil
{
    @Override
    public InventoryView createInventoryView(Inventory top, Inventory bottom, Player player, InventoryType type, String title)
    {
        return new InventoryView()
        {
            @NotNull
            @Override
            public Inventory getTopInventory()
            {
                return top;
            }

            @NotNull
            @Override
            public Inventory getBottomInventory()
            {
                return bottom;
            }

            @NotNull
            @Override
            public HumanEntity getPlayer()
            {
                return player;
            }

            @NotNull
            @Override
            public InventoryType getType()
            {
                return type;
            }

            @Override
            public void setItem(int slot, @Nullable ItemStack item)
            {

            }

            @Nullable
            @Override
            public ItemStack getItem(int slot)
            {
                return null;
            }

            @Override
            public void setCursor(@Nullable ItemStack item)
            {

            }

            @Nullable
            @Override
            public ItemStack getCursor()
            {
                return null;
            }

            @Nullable
            @Override
            public Inventory getInventory(int rawSlot)
            {
                return null;
            }

            @Override
            public int convertSlot(int rawSlot)
            {
                return 0;
            }

            @NotNull
            @Override
            public InventoryType.SlotType getSlotType(int slot)
            {
                return InventoryType.SlotType.CONTAINER;
            }

            @Override
            public void close()
            {

            }

            @Override
            public int countSlots()
            {
                return bottom.getSize() + top.getSize();
            }

            @Override
            public boolean setProperty(@NotNull InventoryView.Property prop, int value)
            {
                return false;
            }

            @NotNull
            @Override
            public String getTitle()
            {
                return title;
            }

            @NotNull
            @Override
            public String getOriginalTitle()
            {
                return title;
            }

            @Override
            public void setTitle(@NotNull String s)
            {
                // Ignore.
            }
        };
    }
}
