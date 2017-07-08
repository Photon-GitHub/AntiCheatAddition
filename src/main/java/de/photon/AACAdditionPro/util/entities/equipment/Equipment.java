package de.photon.AACAdditionPro.util.entities.equipment;

import com.comphenix.protocol.wrappers.EnumWrappers;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.util.entities.ClientsidePlayerEntity;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityEquipment;
import lombok.SneakyThrows;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class Equipment implements Cloneable
{
    /**
     * The {@link HashMap} with all {@link ItemStack}s.
     * All equipmentMap is mapped to the slot it should be applied to.
     */
    private final Map<EnumWrappers.ItemSlot, ItemStack> equipmentMap;

    public Equipment(Material itemInMainHand, Material itemInOffHand, Material helmet, Material chestPlate, Material leggings, Material boots)
    {
        // 6 slots
        equipmentMap = new HashMap<>(6, 1);

        // Hands
        this.equipmentMap.put(EnumWrappers.ItemSlot.MAINHAND, new ItemStack(itemInMainHand));
        this.equipmentMap.put(EnumWrappers.ItemSlot.OFFHAND, new ItemStack(itemInOffHand));

        // Armor
        this.equipmentMap.put(EnumWrappers.ItemSlot.HEAD, new ItemStack(helmet));
        this.equipmentMap.put(EnumWrappers.ItemSlot.CHEST, new ItemStack(chestPlate));
        this.equipmentMap.put(EnumWrappers.ItemSlot.LEGS, new ItemStack(leggings));
        this.equipmentMap.put(EnumWrappers.ItemSlot.FEET, new ItemStack(boots));
    }

    public Equipment(Material itemInMainHand, Material itemInOffHand, Material[] armor)
    {
        // 6 slots
        equipmentMap = new HashMap<>(6, 1);

        // Hands
        this.equipmentMap.put(EnumWrappers.ItemSlot.MAINHAND, new ItemStack(itemInMainHand));
        this.equipmentMap.put(EnumWrappers.ItemSlot.OFFHAND, new ItemStack(itemInOffHand));

        for (Material material : armor) {
            this.equipmentMap.put(EquipmentMapping.getEquipmentMappingOfMaterial(material).getSlotOfItem(), new ItemStack(material));
        }
    }

    /**
     * This method automatically filters out the equipmentMap that will cause errors on this {@link de.photon.AACAdditionPro.util.multiversion.ServerVersion}.
     *
     * @return all the equipmentMap for the current {@link de.photon.AACAdditionPro.util.multiversion.ServerVersion}.
     */
    private Map<EnumWrappers.ItemSlot, ItemStack> getEquipmentForServerVersion()
    {
        switch (AACAdditionPro.getInstance().getServerVersion()) {
            case MC188:
                this.equipmentMap.remove(EnumWrappers.ItemSlot.OFFHAND);
                break;
            case MC110:
            case MC111:
            case MC112:
                break;
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }

        return this.equipmentMap;
    }

    /**
     * Equips the {@link ClientsidePlayerEntity} with this {@link Equipment}.
     */
    public void equipPlayerEntity(ClientsidePlayerEntity playerEntity)
    {
        this.getEquipmentForServerVersion().forEach(
                (slot, itemStack) ->
                {
                    final WrapperPlayServerEntityEquipment entityEquipmentWrapper = new WrapperPlayServerEntityEquipment();

                    entityEquipmentWrapper.setEntityID(playerEntity.getEntityID());
                    entityEquipmentWrapper.setSlot(slot);
                    entityEquipmentWrapper.setItem(itemStack);

                    entityEquipmentWrapper.sendPacket(playerEntity.getObservedPlayer());
                });
    }

    @Override
    @SneakyThrows(CloneNotSupportedException.class)
    public Equipment clone()
    {
        return (Equipment) super.clone();
    }
}
