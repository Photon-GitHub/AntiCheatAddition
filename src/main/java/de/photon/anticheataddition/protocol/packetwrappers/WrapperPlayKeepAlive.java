package de.photon.anticheataddition.protocol.packetwrappers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.anticheataddition.ServerVersion;

public abstract class WrapperPlayKeepAlive extends AbstractPacket
{
    /**
     * Constructs a new strongly typed wrapper for the given packet.
     *
     * @param handle - handle to the raw packet data.
     * @param type   - the packet type.
     */
    protected WrapperPlayKeepAlive(PacketContainer handle, PacketType type)
    {
        super(handle, type);
    }

    /**
     * Retrieve Keep Alive ID.
     *
     * @return The current Keep Alive ID
     */
    public long getKeepAliveId()
    {
        return ServerVersion.is18() ? (long) handle.getIntegers().read(0) : handle.getLongs().read(0);
    }

    /**
     * Set Keep Alive ID.
     *
     * @param value - new value.
     */
    public void setKeepAliveId(long value)
    {
        if (ServerVersion.is18()) handle.getIntegers().write(0, (int) value);
        else handle.getLongs().write(0, value);
    }
}
