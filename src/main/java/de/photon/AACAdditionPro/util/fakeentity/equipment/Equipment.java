package de.photon.AACAdditionPro.util.fakeentity.equipment;

import com.comphenix.protocol.wrappers.EnumWrappers;
import de.photon.AACAdditionPro.util.fakeentity.ClientsideEntity;
import de.photon.AACAdditionPro.util.multiversion.ServerVersion;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityEquipment;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.concurrent.ThreadLocalRandom;

public class Equipment extends EnumMap<EnumWrappers.ItemSlot, Material>
{
    private final ClientsideEntity entity;

    public Equipment(ClientsideEntity entity)
    {
        super(EnumWrappers.ItemSlot.class);
        this.entity = entity;

        // Init
        replaceArmor();
        replaceMainHand();

        if (ServerVersion.getActiveServerVersion() != ServerVersion.MC188)
        {
            replaceOffhand();
        }
    }

    public void replaceMainHand()
    {
        replaceSlot(EnumWrappers.ItemSlot.MAINHAND);
    }

    public void replaceOffhand()
    {
        if (ServerVersion.getActiveServerVersion() != ServerVersion.MC188)
        {
            replaceSlot(EnumWrappers.ItemSlot.OFFHAND);
        }
    }

    public void replaceArmor()
    {
        replaceSlot(EnumWrappers.ItemSlot.HEAD);
        replaceSlot(EnumWrappers.ItemSlot.CHEST);
        replaceSlot(EnumWrappers.ItemSlot.LEGS);
        replaceSlot(EnumWrappers.ItemSlot.FEET);
    }

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

    public void replaceSlot(final EnumWrappers.ItemSlot itemSlot)
    {
        this.put(itemSlot, EquipmentDatabase.instance.getRandomEquipment(this.entity.getObservedPlayer(), itemSlot));
    }

    /**
     * Equips the {@link ClientsideEntity} with this {@link Equipment}.
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
