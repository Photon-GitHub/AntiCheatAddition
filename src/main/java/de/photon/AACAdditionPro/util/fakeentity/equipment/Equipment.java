package de.photon.AACAdditionPro.util.fakeentity.equipment;

import com.comphenix.protocol.wrappers.EnumWrappers;
import de.photon.AACAdditionPro.util.fakeentity.ClientsideEntity;
import de.photon.AACAdditionPro.util.multiversion.ServerVersion;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityEquipment;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class Equipment implements Cloneable
{
    /**
     * The selector is designed to be a singleton. So he can feed multiple inventories at once
     */
    private static final EquipmentSelector selector = new EquipmentSelector( new EquipmentDatabase() );

    /**
     * The {@link HashMap} with all {@link ItemStack}s.
     * All equipmentMap is mapped to the slot it should be applied to.
     */
    private final Map<EnumWrappers.ItemSlot, ItemStack> equipmentMap = new EnumMap<>(EnumWrappers.ItemSlot.class);

    private final ClientsideEntity entity;


    public Equipment(ClientsideEntity entity)
    {
        this.entity = entity;
    }

    /**
     * Equip the entity with new armor
     */
    public void equipArmor()
    {
        // Let the selector get the armor
        Material[] armor = Equipment.selector.selectArmor(this.entity);
        if ( armor != null ) {
            for (int i = 0; i < armor.length; i++) {
                Material armorPiece = armor[i];

                // Security fallback to prevent NPEs
                if (armorPiece == null) armorPiece = Material.AIR;

                EnumWrappers.ItemSlot itemSlot = EquipmentMapping.values()[i].getSlotOfItem();
                ItemStack itemStack = new ItemStack(armorPiece);
                equipItem(itemSlot, itemStack);
            }
        }
    }

    /**
     * Equip the entity with a in hand item (like a sword, blocks etc.)
     */
    public void equipInHand()
    {
        Material mainHand = Equipment.selector.selectMainHand(this.entity);

        // Security fallback to prevent NPEs
        if (mainHand == null) mainHand = Material.AIR;

        ItemStack itemStack = new ItemStack(mainHand);
        equipItem(EnumWrappers.ItemSlot.MAINHAND, itemStack);
    }

    private void equipItem(EnumWrappers.ItemSlot itemSlot, ItemStack itemStack)
    {
        // TODO: Fire a event so the user can change things like durability or custom NBT tags
        this.equipmentMap.put(itemSlot, itemStack);
    }

    /**
     * This method automatically filters out the equipmentMap that will cause errors on this {@link de.photon.AACAdditionPro.util.multiversion.ServerVersion}.
     *
     * @return all the equipmentMap for the current {@link de.photon.AACAdditionPro.util.multiversion.ServerVersion}.
     */
    private Map<EnumWrappers.ItemSlot, ItemStack> getEquipmentForServerVersion()
    {
        switch (ServerVersion.getActiveServerVersion()) {
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
     * Equips the {@link ClientsideEntity} with this {@link Equipment}.
     */
    public void equipPlayerEntity()
    {
        this.getEquipmentForServerVersion().forEach(
                (slot, itemStack) ->
                {
                    final WrapperPlayServerEntityEquipment entityEquipmentWrapper = new WrapperPlayServerEntityEquipment();

                    entityEquipmentWrapper.setEntityID(entity.getEntityID());
                    entityEquipmentWrapper.setSlot(slot);
                    entityEquipmentWrapper.setItem(itemStack);

                    entityEquipmentWrapper.sendPacket(entity.getObservedPlayer());
                });
    }

    @Override
    public Equipment clone() throws CloneNotSupportedException
    {
        return (Equipment) super.clone();
    }

    public ItemStack getMainHand() {
        return this.equipmentMap.get( EnumWrappers.ItemSlot.MAINHAND );
    }

}
