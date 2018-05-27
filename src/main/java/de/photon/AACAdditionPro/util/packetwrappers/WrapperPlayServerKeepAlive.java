package de.photon.AACAdditionPro.util.packetwrappers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.AACAdditionPro.util.multiversion.ServerVersion;

public class WrapperPlayServerKeepAlive extends AbstractPacket
{
    public static final PacketType TYPE = PacketType.Play.Server.KEEP_ALIVE;

    public WrapperPlayServerKeepAlive()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayServerKeepAlive(PacketContainer packet)
    {
        super(packet, TYPE);
    }

    /**
     * Retrieve Keep Alive ID.
     *
     * @return The current Keep Alive ID
     */
    public long getKeepAliveId()
    {
        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
                return handle.getIntegers().read(0);
            case MC110:
            case MC111:
            case MC112:
                return handle.getLongs().read(0);
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }

    /**
     * Set Keep Alive ID.
     *
     * @param value - new value.
     */
    public void setKeepAliveId(long value)
    {
        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
                handle.getIntegers().write(0, (int) value);
            case MC110:
            case MC111:
            case MC112:
                handle.getLongs().write(0, value);
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }
}