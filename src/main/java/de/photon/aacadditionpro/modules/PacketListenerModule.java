package de.photon.aacadditionpro.modules;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import org.bukkit.entity.Player;

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

    /**
     * This safely gets the user from the event.
     *
     * @return the user or null, if the user is not found or invalid.
     */
    static User safeGetUserFromEvent(PacketEvent event)
    {
        // Special handling here as a player could potentially log out after this and therefore cause a NPE.
        final Player player = event.getPlayer();
        if (event.isPlayerTemporary() || player == null) {
            return null;
        }

        return UserManager.getUser(player.getUniqueId());
    }
}
