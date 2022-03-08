package de.photon.aacadditionpro.protocol.packetwrappers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

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
        Preconditions.checkNotNull(handle, "Packet handle cannot be NULL.");
        Preconditions.checkArgument(Objects.equal(handle.getType(), type), handle.getHandle() + " is not a packet of type " + type);
        this.handle = handle;
    }

    public PacketContainer getHandle()
    {
        return handle;
    }
}