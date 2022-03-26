package de.photon.anticheataddition.util.visibility;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.util.datastructure.SetUtil;
import de.photon.anticheataddition.util.messaging.DebugSender;
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
import java.util.stream.Collectors;

abstract class EntityInformationHider implements Listener
{
    private final Multimap<Entity, Entity> hiddenFromPlayerMap;
    private final PacketListener informationPacketListener;

    protected EntityInformationHider(@NotNull PacketType... affectedPackets)
    {
        hiddenFromPlayerMap = MultimapBuilder.hashKeys(AntiCheatAddition.SERVER_EXPECTED_PLAYERS)
                                             .hashSetValues(AntiCheatAddition.WORLD_EXPECTED_PLAYERS)
                                             .build();

        informationPacketListener = new PacketAdapter(AntiCheatAddition.getInstance(), ListenerPriority.NORMAL, Set.of(affectedPackets))
        {
            @Override
            public void onPacketSending(final PacketEvent event)
            {
                if (event.isPlayerTemporary()) return;
                final int entityId = event.getPacket().getIntegers().read(0);

                // Get all hidden entities
                final boolean hidden;
                synchronized (hiddenFromPlayerMap) {
                    // The test for the entityId must happen here in the synchronized block as get only returns a view that might change async.
                    hidden = hiddenFromPlayerMap.get(event.getPlayer()).stream()
                                                .mapToInt(Entity::getEntityId)
                                                .anyMatch(i -> i == entityId);
                }

                if (hidden) {
                    event.setCancelled(true);
                    DebugSender.getInstance().sendDebug("PacketEvent " + event.getPlayer().getName() + " for entity " + entityId + " cancelled.", true, false);
                }
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
        if (ServerVersion.containsActiveServerVersion(this.getSupportedVersions())) {
            // Register events and packet listener
            AntiCheatAddition.getInstance().registerListener(this);
            ProtocolLibrary.getProtocolManager().addPacketListener(this.informationPacketListener);
        }
    }

    public void unregisterListeners()
    {
        // Only stop if the ServerVersion is supported
        if (ServerVersion.containsActiveServerVersion(this.getSupportedVersions())) {
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
        DebugSender.getInstance().sendDebug("ChunkUnload " + entities.stream().map(Entity::getName).collect(Collectors.joining(", ")), true, false);
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
        DebugSender.getInstance().sendDebug("Death " + event.getEntity().getName(), true, false);
        removeEntity(event.getEntity());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event)
    {
        // Creative and Spectator players are ignored by ESP and therefore need to be removed from hiding manually.
        switch (event.getNewGameMode()) {
            case CREATIVE:
            case SPECTATOR:
                DebugSender.getInstance().sendDebug("GameModeChange " + event.getPlayer().getName(), true, false);
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
        DebugSender.getInstance().sendDebug("Quit " + event.getPlayer().getName(), true, false);
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
        if (!toHide.isEmpty())
            DebugSender.getInstance().sendDebug("setHidden " + observer.getName() + " toHide: " + toHide.stream().map(Entity::getName).collect(Collectors.joining(", ")), true, false);

        Set<Entity> oldHidden;
        synchronized (hiddenFromPlayerMap) {
            oldHidden = Set.copyOf(hiddenFromPlayerMap.replaceValues(observer, toHide));
        }

        final Set<Entity> newRevealed = SetUtil.difference(oldHidden, toHide);
        final Set<Entity> newHidden = SetUtil.difference(toHide, oldHidden);

        // Call onHide for those entities that have been revealed and shall now be hidden.
        this.onHide(observer, newHidden);
        return newRevealed;
    }

    protected abstract void onHide(@NotNull Player observer, @NotNull Set<Entity> toHide);

    protected abstract void onReveal(@NotNull Player observer, @NotNull Set<Entity> revealed);
}
