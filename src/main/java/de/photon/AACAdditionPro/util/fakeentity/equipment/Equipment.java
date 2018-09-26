package de.photon.AACAdditionPro.util.fakeentity.equipment;

import com.comphenix.protocol.wrappers.EnumWrappers;
import de.photon.AACAdditionPro.ServerVersion;
import de.photon.AACAdditionPro.util.fakeentity.ClientsideLivingEntity;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityEquipment;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.concurrent.ThreadLocalRandom;

public class Equipment extends EnumMap<EnumWrappers.ItemSlot, Material>
{
    private final ClientsideLivingEntity entity;
    private final boolean offhand;

    public Equipment(ClientsideLivingEntity entity, boolean offhand)
    {
        super(EnumWrappers.ItemSlot.class);
        this.entity = entity;
        this.offhand = offhand;

        // Init
        replaceArmor();
        replaceHands();
    }

    /**
     * This method will replace the items in the mainhand and offhand. The latter will only be affected if offhand handling was
     * enabled in the constructor. <p></p>
     * Make sure you call {@link #updateEquipment()} afterwards for the changes to take effect.
     */
    public void replaceHands()
    {
        replaceSlot(EnumWrappers.ItemSlot.MAINHAND);

        if (offhand)
        {
            replaceSlot(EnumWrappers.ItemSlot.OFFHAND);
        }
    }

    /**
     * This method will replace all armor slots. <p></p>
     * Make sure you call {@link #updateEquipment()} afterwards for the changes to take effect.
     */
    public void replaceArmor()
    {
        replaceSlot(EnumWrappers.ItemSlot.HEAD);
        replaceSlot(EnumWrappers.ItemSlot.CHEST);
        replaceSlot(EnumWrappers.ItemSlot.LEGS);
        replaceSlot(EnumWrappers.ItemSlot.FEET);
    }

    /**
     * This method will replace one randomly chosen armor slot. <p></p>
     * Make sure you call {@link #updateEquipment()} afterwards for the changes to take effect.
     */
    public void replaceRandomArmorPiece()
    {
        switch (ThreadLocalRandom.current().nextInt(4))
        {
            case 0:
                replaceSlot(EnumWrappers.ItemSlot.HEAD);
                break;
            case 1:
                replaceSlot(EnumWrappers.ItemSlot.CHEST);
                break;
            case 2:
                replaceSlot(EnumWrappers.ItemSlot.LEGS);
                break;
            case 3:
                replaceSlot(EnumWrappers.ItemSlot.FEET);
                break;
        }
    }

    /**
     * This method will replace one slot as defined in {@link com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot}
     * with a randomly chosen piece of equipment. <p></p>
     * Make sure you call {@link #updateEquipment()} afterwards for the changes to take effect.
     */
    private void replaceSlot(final EnumWrappers.ItemSlot itemSlot)
    {
        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
            case MC111:
            case MC112:
                this.put(itemSlot, LegacyEquipmentDatabase.INSTANCE.getRandomEquipment(this.entity.getObservedPlayer(), itemSlot));
                break;
            case MC113:
                this.put(itemSlot, EquipmentDatabase.INSTANCE.getRandomEquipment(this.entity.getObservedPlayer(), itemSlot));
                break;
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }

    /**
     * Equips the {@link ClientsideLivingEntity} with this {@link Equipment}.
     */
    public void updateEquipment()
    {
        this.forEach(
                (slot, material) ->
                {
                    final WrapperPlayServerEntityEquipment entityEquipmentWrapper = new WrapperPlayServerEntityEquipment();

                    entityEquipmentWrapper.setEntityID(entity.getEntityID());
                    entityEquipmentWrapper.setSlot(slot);
                    entityEquipmentWrapper.setItem(new ItemStack(material));

                    entityEquipmentWrapper.sendPacket(entity.getObservedPlayer());
                });
    }
}
