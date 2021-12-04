package de.photon.aacadditionpro.protocol.packetwrappers.sentbyserver;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.aacadditionpro.ServerVersion;

public class WrapperPlayServerRelEntityMoveLook extends WrapperPlayServerRelEntityMove implements IWrapperPlayServerLook
{
    public static final PacketType TYPE = PacketType.Play.Server.REL_ENTITY_MOVE_LOOK;

    public WrapperPlayServerRelEntityMoveLook()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayServerRelEntityMoveLook(PacketContainer packet)
    {
        super(packet, TYPE);
    }

    @Override
    public int getByteOffset()
    {
        return ServerVersion.getActiveServerVersion() == ServerVersion.MC18 ? 3 : 0;
    }

    @Override
    public boolean getOnGround()
    {
        return this.getHandle().getBooleans().read(0);
    }

    @Override
    public void setOnGround(final boolean value)
    {
        this.getHandle().getBooleans().write(0, value);
    }
}
