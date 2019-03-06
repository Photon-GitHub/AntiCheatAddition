package de.photon.AACAdditionPro.util.packetwrappers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.AACAdditionPro.ServerVersion;

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
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
            case MC111:
                return handle.getIntegers().read(0);
            case MC112:
            case MC113:
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
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
            case MC111:
                handle.getIntegers().write(0, (int) value);
                break;
            case MC112:
            case MC113:
                handle.getLongs().write(0, value);
                break;
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }
}
