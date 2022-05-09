package de.photon.anticheataddition.protocol.packetwrappers.sentbyserver.entitydestroy;

import com.comphenix.protocol.events.PacketContainer;
import de.photon.anticheataddition.protocol.packetwrappers.AbstractPacket;

import java.util.ArrayList;
import java.util.List;

public class LegacyWrapperPlayServerEntityDestroy extends AbstractPacket implements IWrapperServerEntityDestroy
{
    public LegacyWrapperPlayServerEntityDestroy()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public LegacyWrapperPlayServerEntityDestroy(PacketContainer packet)
    {
        super(packet, TYPE);
    }

    public List<Integer> getEntityIDs()
    {
        final int[] array = handle.getIntegerArrays().read(0);
        final List<Integer> list = new ArrayList<>();

        for (int i : array) list.add(i);
        return list;
    }

    public void setEntityIds(List<Integer> value)
    {
        final int[] array = new int[value.size()];
        int index = 0;
        for (int i : value) array[index++] = i;

        handle.getIntegerArrays().write(0, array);
    }
}