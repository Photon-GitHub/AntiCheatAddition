package de.photon.AACAdditionPro.util.clientsideentities.equipment;

import com.comphenix.protocol.wrappers.EnumWrappers;
import de.photon.AACAdditionPro.util.clientsideentities.ClientsideEntity;
import de.photon.AACAdditionPro.util.clientsideentities.ClientsidePlayerEntity;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityEquipment;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class EntityEquipmentUtils
{
    private static final EnumWrappers.ItemSlot[] armorSlots = {
            EnumWrappers.ItemSlot.HEAD,
            EnumWrappers.ItemSlot.CHEST,
            EnumWrappers.ItemSlot.LEGS,
            EnumWrappers.ItemSlot.FEET
    };

    private static final EnumWrappers.ItemSlot handSlot = EnumWrappers.ItemSlot.MAINHAND;

    /**
     * This equips a {@link ClientsideEntity} with the according randomized items.
     *
     * @param receiver      the player who can see the {@link ClientsideEntity}
     * @param playerEntity  the {@link ClientsideEntity} itself
     * @param equipmentType what parts of the {@link ClientsideEntity} should be equipped
     */
    public static void equipPlayerEntity(final Player receiver, final ClientsidePlayerEntity playerEntity, final EquipmentType equipmentType)
    {
        // Material for the hand, not needed for ARMOR.
        Material handMaterial = Material.AIR;

        // The different equipmentTypes need different behaviour
        switch (equipmentType) {
            case ARMOR:
                // Get the armor
                final Material[] armor = EntityEquipmentDatabase.getRandomArmorMaterials();

                // Equip the armor with Packets
                for (byte b = 0; b < 4; b++) {
                    final WrapperPlayServerEntityEquipment entityEquipmentWrapper = new WrapperPlayServerEntityEquipment();

                    entityEquipmentWrapper.setEntityID(playerEntity.getEntityID());
                    entityEquipmentWrapper.setSlot(armorSlots[b]);
                    entityEquipmentWrapper.setItem(new ItemStack(armor[b]));

                    entityEquipmentWrapper.sendPacket(receiver);
                }
                // Return here, the other equipmentTypes need more than the switch-statement
                return;
            case WEAPON:
                handMaterial = EntityEquipmentDatabase.getRandomWeaponMaterial();
                break;
            case NORMAL:
                handMaterial = EntityEquipmentDatabase.getRandomNormalMaterial();
                break;
        }

        // Hand - Equipment
        final WrapperPlayServerEntityEquipment entityEquipmentWrapper = new WrapperPlayServerEntityEquipment();

        entityEquipmentWrapper.setEntityID(playerEntity.getEntityID());
        entityEquipmentWrapper.setSlot(handSlot);
        entityEquipmentWrapper.setItem(new ItemStack(handMaterial));

        entityEquipmentWrapper.sendPacket(receiver);
    }
}
