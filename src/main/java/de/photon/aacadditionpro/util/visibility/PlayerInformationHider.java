package de.photon.aacadditionpro.util.visibility;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.ServerVersion;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

abstract class PlayerInformationHider implements Listener
{
    private final PacketListener informationPacketListener;
    private final Multimap<Integer, Integer> hiddenFromPlayerMap;

    protected PlayerInformationHider(@NotNull PacketType... affectedPackets)
    {
        informationPacketListener = new PacketAdapter(AACAdditionPro.getInstance(), ListenerPriority.NORMAL, Set.of(affectedPackets))
        {
            @Override
            public void onPacketSending(final PacketEvent event)
            {
                if (event.isPlayerTemporary()) return;
                val entityID = event.getPacket().getIntegers().read(0);

                final boolean hidden;
                synchronized (hiddenFromPlayerMap) {
                    hidden = hiddenFromPlayerMap.containsEntry(event.getPlayer().getEntityId(), entityID);
                }

                if (hidden) event.setCancelled(true);
            }
        };

        hiddenFromPlayerMap = MultimapBuilder.hashKeys(AACAdditionPro.SERVER_EXPECTED_PLAYERS).hashSetValues(AACAdditionPro.WORLD_EXPECTED_PLAYERS).build();
    }

    public void clear()
    {
        synchronized (hiddenFromPlayerMap) {
            hiddenFromPlayerMap.clear();
        }
    }

    public void registerListeners()
    {
        // Only start if the ServerVersion is supported
        if (ServerVersion.containsActiveServerVersion(this.getSupportedVersions())) {
            // Register events and packet listener
            AACAdditionPro.getInstance().registerListener(this);
            ProtocolLibrary.getProtocolManager().addPacketListener(this.informationPacketListener);
        }
    }

    public void unregisterListeners()
    {
        HandlerList.unregisterAll(this);
        ProtocolLibrary.getProtocolManager().removePacketListener(this.informationPacketListener);
    }

    protected Set<ServerVersion> getSupportedVersions()
    {
        return EnumSet.allOf(ServerVersion.class);
    }

    @EventHandler
    public void onEntityDeath(final EntityDeathEvent event)
    {
        removeEntity(event.getEntity());
    }

    @EventHandler
    public void onChunkUnload(final ChunkUnloadEvent event)
    {
        // Cache entities for performance reasons so the server doesn't need to load them again when the
        // task is executed.
        synchronized (hiddenFromPlayerMap) {
            for (final Entity entity : event.getChunk().getEntities()) removeEntity(entity);
        }
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event)
    {
        removeEntity(event.getPlayer());
    }


    /**
     * Remove the given entity from the underlying map.
     *
     * @param entity - the entity to remove.
     */
    private void removeEntity(final Entity entity)
    {
        val entityId = entity.getEntityId();
        synchronized (hiddenFromPlayerMap) {
            hiddenFromPlayerMap.removeAll(entityId);
            // Remove all the instances of entity from the values.
            //noinspection StatementWithEmptyBody
            while (hiddenFromPlayerMap.values().remove(entityId)) ;
        }
    }

    /**
     * Hides a {@link Player} from another {@link Player}.
     */
    public void hidePlayer(final Player observer, final Player playerToHide)
    {
        if (observer == null || playerToHide == null) return;

        synchronized (hiddenFromPlayerMap) {
            hiddenFromPlayerMap.put(observer.getEntityId(), playerToHide.getEntityId());
        }

        this.onHide(observer, playerToHide);
    }

    protected abstract void onHide(@NotNull Player observer, @NotNull Player playerToHide);

    /**
     * Reveals a {@link Player} from another {@link Player}.
     */
    public void revealPlayer(final Player observer, final Player playerToReveal)
    {
        if (observer == null || playerToReveal == null) return;

        boolean hiddenBefore;
        synchronized (hiddenFromPlayerMap) {
            hiddenBefore = hiddenFromPlayerMap.remove(observer.getEntityId(), playerToReveal.getEntityId());
        }

        // Resend packets
        if (ProtocolLibrary.getProtocolManager() != null && hiddenBefore) {
            Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> ProtocolLibrary.getProtocolManager().updateEntity(playerToReveal, List.of(observer)));
        }
    }
}
