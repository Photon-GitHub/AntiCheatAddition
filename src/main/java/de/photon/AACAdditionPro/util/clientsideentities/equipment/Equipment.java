package de.photon.AACAdditionPro.util.clientsideentities.equipment;

import lombok.SneakyThrows;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Equipment implements Cloneable
{

    private ItemStack itemInMainHand;
    private ItemStack itemInOffHand;
    private ItemStack helmet;
    private ItemStack chestPlate;
    private ItemStack leggings;
    private ItemStack boots;

    public ItemStack getItemInMainHand()
    {
        return itemInMainHand.clone();
    }

    public void setItemInMainHand(ItemStack itemStack)
    {
        itemInMainHand = itemStack.clone();
    }

    public ItemStack getItemInOffHand()
    {
        return itemInOffHand.clone();
    }

    public void setItemInOffHand(ItemStack itemStack)
    {
        itemInOffHand = itemStack.clone();
    }

    public ItemStack getItemInHand()
    {
        return getItemInMainHand();
    }

    public void setItemInHand(ItemStack itemStack)
    {
        setItemInMainHand(itemStack);
    }

    public ItemStack getHelmet()
    {
        return helmet.clone();
    }

    public void setHelmet(ItemStack itemStack)
    {
        helmet = itemStack.clone();
    }

    public ItemStack getChestplate()
    {
        return chestPlate.clone();
    }

    public void setChestplate(ItemStack itemStack)
    {
        chestPlate = itemStack.clone();
    }

    public ItemStack getLeggings()
    {
        return leggings.clone();
    }

    public void setLeggings(ItemStack itemStack)
    {
        leggings = itemStack.clone();
    }

    public ItemStack getBoots()
    {
        return boots.clone();
    }

    public void setBoots(ItemStack itemStack)
    {
        boots = itemStack.clone();
    }

    public void clear()
    {
        ItemStack air = new ItemStack(Material.AIR);
        this.itemInMainHand = air;
        this.itemInOffHand = air;
        this.helmet = air;
        this.chestPlate = air;
        this.leggings = air;
        this.boots = air;
    }

    public ItemStack[] getEquipment(boolean offhandItemIncluded)
    {
        if (offhandItemIncluded) {
            return new ItemStack[]{
                    itemInMainHand,
                    itemInOffHand,
                    helmet,
                    chestPlate,
                    leggings,
                    boots
            };
        } else {
            return new ItemStack[]{
                    itemInMainHand,
                    helmet,
                    chestPlate,
                    leggings,
                    boots
            };
        }
    }

    @Override
    @SneakyThrows(CloneNotSupportedException.class)
    public Equipment clone()
    {
        return (Equipment) super.clone();
    }
}
