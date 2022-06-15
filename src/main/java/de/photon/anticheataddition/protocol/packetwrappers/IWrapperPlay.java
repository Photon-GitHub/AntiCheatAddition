package de.photon.anticheataddition.protocol.packetwrappers;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.entity.Player;

public interface IWrapperPlay
{
    /**
     * Retrieve a handle to the raw packet data.
     *
     * @return Raw packet data.
     */
    PacketContainer getHandle();

    /**
     * Send the current packet to the given receiver.
     *
     * @param receiver - the receiver.
     *
     * @throws RuntimeException If the packet cannot be sent.
     */
    default void sendPacket(Player receiver)
    {
        ProtocolLibrary.getProtocolManager().sendServerPacket(receiver, getHandle());
    }

    /**
     * Send the current packet to all online players.
     */
    default void broadcastPacket()
    {
        ProtocolLibrary.getProtocolManager().broadcastServerPacket(getHandle());
    }
}
