package de.photon.anticheataddition.util.inventoryview;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;

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
