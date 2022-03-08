package de.photon.aacadditionpro.protocol.packetwrappers;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;

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
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(receiver, getHandle());
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Cannot send packet.", e);
        }
    }

    /**
     * Send the current packet to all online players.
     */
    default void broadcastPacket()
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
    default void receivePacket(Player sender)
    {
        try {
            ProtocolLibrary.getProtocolManager().recieveClientPacket(sender, getHandle());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot receive packet.", e);
        }
    }
}
