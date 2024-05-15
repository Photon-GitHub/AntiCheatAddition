package de.photon.anticheataddition.modules.additions.informationhider;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.log.Log;

public class DurabilityHider extends InformationHiderModule
{
    public static final DurabilityHider INSTANCE = new DurabilityHider();

    private DurabilityHider()
    {
        super("InformationHider.parts.durability");
    }

    @Override
    protected void hideInformation(User user, WrapperPlayServerEntityEquipment wrapper, int entityId, ItemStack item)
    {
        Log.finer(() -> "DurabilityHider entity id: " + entityId + " with item " + item);

        if (item.getDamageValue() <= 0) return;
        Log.finer(() -> "Hiding durability " + item + " for entity id " + entityId);
        item.setDamageValue(item.getMaxDamage());
    }
}
