package de.photon.anticheataddition.util.visibility.legacy;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.util.datastructure.SetUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

abstract class PlayerInformationHider implements Listener
{
    private final SetMultimap<Player, Player> hiddenFromPlayerMap;

    protected PlayerInformationHider(@NotNull PacketType... affectedPackets)
    {
        hiddenFromPlayerMap = MultimapBuilder.hashKeys(AntiCheatAddition.SERVER_EXPECTED_PLAYERS)
                                             .hashSetValues(AntiCheatAddition.WORLD_EXPECTED_PLAYERS)
                                             .build();

        // Only start if the ServerVersion is supported
        if (!ServerVersion.containsActive(this.getSupportedVersions())) return;

        // Register events
        AntiCheatAddition.getInstance().registerListener(this);

        if (affectedPackets.length == 0) return;

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

    public void clear()
    {
        synchronized (hiddenFromPlayerMap) {
            hiddenFromPlayerMap.clear();
        }
    }

    protected Set<ServerVersion> getSupportedVersions()
    {
        return ServerVersion.ALL_SUPPORTED_VERSIONS;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event)
    {
        // Creative and Spectator players are ignored by ESP and therefore need to be removed from hiding manually.
        switch (event.getNewGameMode()) {
            case CREATIVE, SPECTATOR -> {
                setHiddenEntities(event.getPlayer(), Set.of());
                removePlayer(event.getPlayer());

                // Run with delay, so we avoid any updates that are underway async.
                Bukkit.getScheduler().runTaskLater(AntiCheatAddition.getInstance(),
                                                   () -> ProtocolLibrary.getProtocolManager().updateEntity(event.getPlayer(), event.getPlayer().getWorld().getPlayers()), 20L);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event)
    {
        removePlayer(event.getPlayer());
    }

    /**
     * Remove the given entity from the underlying map.
     *
     * @param entity - the entity to remove.
     */
    private void removePlayer(Player entity)
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
     */
    public void setHiddenEntities(@NotNull Player observer, @NotNull Set<Player> toHide)
    {
        final Set<Player> oldHidden;
        final Set<Player> newlyHidden;
        synchronized (hiddenFromPlayerMap) {
            oldHidden = Set.copyOf(hiddenFromPlayerMap.get(observer));
            newlyHidden = SetUtil.difference(toHide, oldHidden);

            onPreHide(observer, newlyHidden);

            hiddenFromPlayerMap.replaceValues(observer, toHide);
        }

        // Call onHide for those entities that have been revealed and shall now be hidden.
        this.onHide(observer, newlyHidden);
        this.onReveal(observer, SetUtil.difference(oldHidden, toHide));
    }

    protected void onPreHide(@NotNull Player observer, @NotNull Set<Player> toHide) {}

    protected void onHide(@NotNull Player observer, @NotNull Set<Player> toHide) {}

    protected abstract void onReveal(@NotNull Player observer, @NotNull Set<Player> revealed);
}
