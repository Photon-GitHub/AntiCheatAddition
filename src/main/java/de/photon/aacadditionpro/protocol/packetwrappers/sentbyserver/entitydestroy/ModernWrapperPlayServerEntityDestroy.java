package de.photon.aacadditionpro.protocol.packetwrappers.sentbyserver.entitydestroy;

import com.comphenix.protocol.events.PacketContainer;
import de.photon.aacadditionpro.protocol.packetwrappers.AbstractPacket;

import java.util.List;

public class ModernWrapperPlayServerEntityDestroy extends AbstractPacket implements IWrapperServerEntityDestroy
{
    public ModernWrapperPlayServerEntityDestroy()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public List<Integer> getEntityIDs()
    {
        return handle.getIntLists().read(0);
    }

    @Override
    public void setEntityIds(List<Integer> value)
    {
        handle.getIntLists().write(0, value);
    }
}
