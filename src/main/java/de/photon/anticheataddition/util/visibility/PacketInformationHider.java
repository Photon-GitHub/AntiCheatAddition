package de.photon.anticheataddition.util.visibility;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.ServerVersion;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public abstract class PacketInformationHider extends PlayerInformationHider implements Listener
{
    protected PacketInformationHider(@NotNull PacketType... affectedPackets)
    {
        super();

        // Only start if the ServerVersion is supported
        if (!ServerVersion.containsActive(this.getSupportedVersions()) || affectedPackets.length == 0) return;

        // Get all hidden entities
        // The test for the entityId must happen here in the synchronized block as get only returns a view that might change async.
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(AntiCheatAddition.getInstance(), affectedPackets)
        {
            @Override
            public void onPacketSending(PacketEvent event)
            {
                if (event.isPlayerTemporary() || event.isCancelled()) return;
                final int entityId = event.getPacket().getIntegers().read(0);

                // Get all hidden entities
                final boolean hidden;
                synchronized (hiddenFromPlayerMap) {
                    // The test for the entityId must happen here in the synchronized block as get only returns a view that might change async.
                    hidden = hiddenFromPlayerMap.get(event.getPlayer()).stream()
                                                .mapToInt(Player::getEntityId)
                                                .anyMatch(i -> i == entityId);
                }

                if (hidden) event.setCancelled(true);
            }
        });
    }
}
