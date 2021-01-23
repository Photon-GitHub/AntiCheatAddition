package de.photon.aacadditionproold.util.packetwrappers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;

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

    /**
     * Retrieve a handle to the raw packet data.
     *
     * @return Raw packet data.
     */
    public PacketContainer getHandle()
    {
        return handle;
    }

    /**
     * Send the current packet to the given receiver.
     *
     * @param receiver - the receiver.
     *
     * @throws RuntimeException If the packet cannot be sent.
     */
    public void sendPacket(Player receiver)
    {
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(receiver, getHandle());
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Cannot send packet.", e);
        }
    }

    /**
     * Send the current packet to all online players.
     */
    public void broadcastPacket()
    {
        ProtocolLibrary.getProtocolManager().broadcastServerPacket(getHandle());
    }

    /**
     * Simulate receiving the current packet from the given sender.
     *
     * @param sender - the sender.
     *
     * @throws RuntimeException if the packet cannot be received.
     */
    public void receivePacket(Player sender)
    {
        try {
            ProtocolLibrary.getProtocolManager().recieveClientPacket(sender, getHandle());
        } catch (Exception e) {
            throw new RuntimeException("Cannot receive packet.", e);
        }
    }
}