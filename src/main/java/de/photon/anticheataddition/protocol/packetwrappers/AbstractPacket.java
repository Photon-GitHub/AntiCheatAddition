package de.photon.anticheataddition.protocol.packetwrappers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.google.common.base.Objects;

public abstract class AbstractPacket implements IWrapperPlay
{
    // The packet we will be modifying
    protected final PacketContainer handle;

    /**
     * Constructs a new strongly typed wrapper for the given packet.
     *
     * @param handle - handle to the raw packet data.
     * @param type   - the packet type.
     */
    protected AbstractPacket(PacketContainer handle, PacketType type)
    {
        // Make sure we're given a valid packet
        // No Preconditions here, this is a performance critical constructor as of profiling.
        if (handle == null) throw new NullPointerException("Packet handle cannot be NULL.");
        if (!Objects.equal(handle.getType(), type)) throw new IllegalArgumentException(handle.getHandle() + " is not a packet of type " + type);
        this.handle = handle;
    }

    public PacketContainer getHandle()
    {
        return handle;
    }
}