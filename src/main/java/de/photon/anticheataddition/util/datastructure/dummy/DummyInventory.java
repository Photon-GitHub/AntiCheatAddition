package de.photon.anticheataddition.util.datastructure.dummy;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

public final class DummyInventory implements Inventory
{
    @Override
    public int getSize()
    {
        return 0;
    }

    @Override
    public int getMaxStackSize()
    {
        return 0;
    }

    @Override
    public void setMaxStackSize(int size)
    {
        // Dummy
    }

    @Nullable
    @Override
    public ItemStack getItem(int index)
    {
        return null;
    }

    @Override
    public void setItem(int index, @Nullable ItemStack item)
    {
        // Dummy
    }

    @NotNull
    @Override
    public HashMap<Integer, ItemStack> addItem(@NotNull ItemStack... items) throws IllegalArgumentException
    {
        return new HashMap<>();
    }

    @NotNull
    @Override
    public HashMap<Integer, ItemStack> removeItem(@NotNull ItemStack... items) throws IllegalArgumentException
    {
        return new HashMap<>();
    }

    @NotNull
    @Override
    public ItemStack[] getContents()
    {
        return new ItemStack[0];
    }

    @Override
    public void setContents(@NotNull ItemStack[] items) throws IllegalArgumentException
    {
        // Dummy
    }

    @NotNull
    @Override
    public ItemStack[] getStorageContents()
    {
        return new ItemStack[0];
    }

    @Override
    public void setStorageContents(@NotNull ItemStack[] items) throws IllegalArgumentException
    {
        // Dummy
    }

    @Override
    public boolean contains(@NotNull Material material) throws IllegalArgumentException
    {
        return false;
    }

    @Override
    public boolean contains(@Nullable ItemStack item)
    {
        return false;
    }

    @Override
    public boolean contains(@NotNull Material material, int amount) throws IllegalArgumentException
    {
        return false;
    }

    @Override
    public boolean contains(@Nullable ItemStack item, int amount)
    {
        return false;
    }

    @Override
    public boolean containsAtLeast(@Nullable ItemStack item, int amount)
    {
        return false;
    }

    @NotNull
    @Override
    public HashMap<Integer, ? extends ItemStack> all(@NotNull Material material) throws IllegalArgumentException
    {
        return new HashMap<>();
    }

    @NotNull
    @Override
    public HashMap<Integer, ? extends ItemStack> all(@Nullable ItemStack item)
    {
        return new HashMap<>();
    }

    @Override
    public int first(@NotNull Material material) throws IllegalArgumentException
    {
        return 0;
    }

    @Override
    public int first(@NotNull ItemStack item)
    {
        return 0;
    }

    @Override
    public int firstEmpty()
    {
        return 0;
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }

    @Override
    public void remove(@NotNull Material material) throws IllegalArgumentException
    {
        // Dummy
    }

    @Override
    public void remove(@NotNull ItemStack item)
    {
        // Dummy
    }

    @Override
    public void clear(int index)
    {
        // Dummy
    }

    @Override
    public void clear()
    {
        // Dummy
    }

    @NotNull
    @Override
    public List<HumanEntity> getViewers()
    {
        return new ArrayList<>();
    }

    @NotNull
    @Override
    public InventoryType getType()
    {
        return InventoryType.CHEST;
    }

    @Nullable
    @Override
    public InventoryHolder getHolder()
    {
        return null;
    }

    @NotNull
    @Override
    public ListIterator<ItemStack> iterator()
    {
        return new ArrayList<ItemStack>().listIterator();
    }

    @NotNull
    @Override
    public ListIterator<ItemStack> iterator(int index)
    {
        return new ArrayList<ItemStack>().listIterator();
    }

    @Nullable
    @Override
    public Location getLocation()
    {
        return null;
    }
}
