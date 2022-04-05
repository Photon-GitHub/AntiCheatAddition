package de.photon.anticheataddition.util.visibility.legacy;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.util.datastructure.SetUtil;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Set;

abstract class EntityInformationHider implements Listener
{
    private final Multimap<Entity, Entity> hiddenFromPlayerMap;
    private final PacketListener informationPacketListener;

    protected EntityInformationHider(@NotNull PacketType... affectedPackets)
    {
        hiddenFromPlayerMap = MultimapBuilder.hashKeys(AntiCheatAddition.SERVER_EXPECTED_PLAYERS)
                                             .hashSetValues(AntiCheatAddition.WORLD_EXPECTED_PLAYERS)
                                             .build();

        informationPacketListener = new PacketAdapter(AntiCheatAddition.getInstance(), affectedPackets)
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
                                                .mapToInt(Entity::getEntityId)
                                                .anyMatch(i -> i == entityId);
                }

                if (hidden) event.setCancelled(true);
            }
        };
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
        if (ServerVersion.containsActive(this.getSupportedVersions())) {
            // Register events and packet listener
            AntiCheatAddition.getInstance().registerListener(this);
            ProtocolLibrary.getProtocolManager().addPacketListener(this.informationPacketListener);
        }
    }

    public void unregisterListeners()
    {
        // Only stop if the ServerVersion is supported
        if (ServerVersion.containsActive(this.getSupportedVersions())) {
            HandlerList.unregisterAll(this);
            ProtocolLibrary.getProtocolManager().removePacketListener(this.informationPacketListener);
        }
    }

    protected Set<ServerVersion> getSupportedVersions()
    {
        return ServerVersion.ALL_SUPPORTED_VERSIONS;
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event)
    {
        // Cache entities for performance reasons so the server doesn't need to load them again when the task is executed.
        val entities = Arrays.asList(event.getChunk().getEntities());
        synchronized (hiddenFromPlayerMap) {
            // All the entities that are keys in the map, do this first to reduce the amount of values in the call below.
            for (final Entity entity : entities) hiddenFromPlayerMap.removeAll(entity);

            // Any entities that are values in the map.
            hiddenFromPlayerMap.values().removeAll(entities);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event)
    {
        removeEntity(event.getEntity());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event)
    {
        // Creative and Spectator players are ignored by ESP and therefore need to be removed from hiding manually.
        switch (event.getNewGameMode()) {
            case CREATIVE:
            case SPECTATOR:
                removeEntity(event.getPlayer());
                Bukkit.getScheduler().runTask(AntiCheatAddition.getInstance(), () ->
                        ProtocolLibrary.getProtocolManager().updateEntity(event.getPlayer(), event.getPlayer().getWorld().getPlayers()));
                break;
            default: break;
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event)
    {
        removeEntity(event.getPlayer());
    }

    /**
     * Remove the given entity from the underlying map.
     *
     * @param entity - the entity to remove.
     */
    private void removeEntity(Entity entity)
    {
        synchronized (hiddenFromPlayerMap) {
            hiddenFromPlayerMap.removeAll(entity);
            // Remove all the instances of entity from the values.
            //noinspection StatementWithEmptyBody
            while (hiddenFromPlayerMap.values().remove(entity)) ;
        }
    }

    /**
     * Hides entities from a {@link Player}.
     *
     * @return the {@link Set} of entities that needs updating (newly revealed).
     */
    public Set<Entity> setHiddenEntities(@NotNull Player observer, @NotNull Set<Entity> toHide)
    {
        final Set<Entity> oldHidden;
        final Set<Entity> newlyHidden;
        synchronized (hiddenFromPlayerMap) {
            oldHidden = Set.copyOf(hiddenFromPlayerMap.get(observer));
            newlyHidden = SetUtil.difference(toHide, oldHidden);

            onPreHide(observer, newlyHidden);

            hiddenFromPlayerMap.replaceValues(observer, toHide);
        }

        // Call onHide for those entities that have been revealed and shall now be hidden.
        this.onHide(observer, newlyHidden);

        return SetUtil.difference(oldHidden, toHide);
    }

    protected void onPreHide(@NotNull Player observer, @NotNull Set<Entity> toHide) {}

    protected void onHide(@NotNull Player observer, @NotNull Set<Entity> toHide) {}

    protected abstract void onReveal(@NotNull Player observer, @NotNull Set<Entity> revealed);
}
