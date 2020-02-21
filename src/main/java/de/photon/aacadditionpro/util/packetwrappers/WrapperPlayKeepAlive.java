package de.photon.aacadditionpro.util.packetwrappers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.util.exceptions.UnknownMinecraftVersion;

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
                return handle.getIntegers().read(0);
            case MC112:
            case MC113:
            case MC114:
            case MC115:
                return handle.getLongs().read(0);
            default:
                throw new UnknownMinecraftVersion();
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
                handle.getIntegers().write(0, (int) value);
                break;
            case MC112:
            case MC113:
            case MC114:
            case MC115:
                handle.getLongs().write(0, value);
                break;
            default:
                throw new UnknownMinecraftVersion();
        }
    }
}
