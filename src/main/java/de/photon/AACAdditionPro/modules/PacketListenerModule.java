package de.photon.AACAdditionPro.modules;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketListener;

/**
 * Marks a {@link Module} which utilizes the {@link PacketListener} functionality.
 */
public interface PacketListenerModule extends Module, PacketListener
{
    /**
     * Additional chores needed to enable a {@link PacketListenerModule}
     */
    static void enable(final PacketListenerModule module)
    {
        ProtocolLibrary.getProtocolManager().addPacketListener(module);
    }

    /**
     * Additional chores needed to disable a {@link PacketListenerModule}
     */
    static void disable(final PacketListenerModule module)
    {
        ProtocolLibrary.getProtocolManager().removePacketListener(module);
    }
}
