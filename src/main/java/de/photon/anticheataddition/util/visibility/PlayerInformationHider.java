package de.photon.anticheataddition.util.visibility;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.MojangAPIUtil;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.util.datastructure.SetUtil;
import de.photon.anticheataddition.util.messaging.Log;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public abstract class PlayerInformationHider implements Listener
{
    private static final long SKIN_CACHE_CAPACITY = 2048L;
    private static final LoadingCache<UUID, List<TextureProperty>> SKIN_CACHE = CacheBuilder.newBuilder()
                                                                                            .initialCapacity(AntiCheatAddition.SERVER_EXPECTED_PLAYERS)
                                                                                            .maximumSize(SKIN_CACHE_CAPACITY)
                                                                                            .build(new CacheLoader<>()
                                                                                            {
                                                                                                @Override
                                                                                                public @NotNull List<TextureProperty> load(@NotNull UUID key)
                                                                                                {
                                                                                                    try {
                                                                                                        final var skin = MojangAPIUtil.requestPlayerTextureProperties(key);
                                                                                                        if (skin == null) return List.of();
                                                                                                        return skin;
                                                                                                    } catch (IllegalStateException exception) {
                                                                                                        return List.of();
                                                                                                    }
                                                                                                }
                                                                                            });

    protected final SetMultimap<Player, Player> hiddenFromPlayerMap;

    protected PlayerInformationHider()
    {
        hiddenFromPlayerMap = MultimapBuilder.hashKeys(AntiCheatAddition.SERVER_EXPECTED_PLAYERS)
                                             .hashSetValues(AntiCheatAddition.WORLD_EXPECTED_PLAYERS)
                                             .build();

        // Only start if the ServerVersion is supported
        if (!ServerVersion.containsActive(this.getSupportedVersions())) return;

        // Register events
        AntiCheatAddition.getInstance().registerListener(this);
    }

    protected Set<ServerVersion> getSupportedVersions()
    {
        return ServerVersion.ALL_SUPPORTED_VERSIONS;
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event)
    {
        // An observer that has hidden players and changes their GameMode to creative or spectator is no longer processed by ESP, and therefore, their hidden players must be revealed manually.
        // On the contrary, all other players are still processed and the hider will automatically reveal the player with changed GameMode.
        switch (event.getNewGameMode()) {
            case CREATIVE, SPECTATOR -> setHiddenEntities(event.getPlayer(), Set.of());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event)
    {
        removePlayer(event.getPlayer());
    }

    /**
     * Remove the given entity from the underlying map.
     * This method is thread-safe.
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
     * This method is thread-safe.
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


    /**
     * Sends the observing {@link Player} a packet to add the watched {@link Player} to the tablist.
     */
    public static void addWatchedPlayerToTablistPacket(Player observer, Player watched)
    {
        List<TextureProperty> skin = null;
        try {
            skin = SKIN_CACHE.get(watched.getUniqueId());
        } catch (ExecutionException e) {
            // Server unreachable, or the player does not exist. -> Ignore
            Log.finer(() -> "Failed to load skin for player " + watched.getName() + ". Error: " + e.getMessage());
        }

        if (skin == null || skin.isEmpty()) return;

        Log.finer(() -> "Adding player " + watched.getName() + " to the tablist of " + observer.getName() + ". Skin successfully loaded.");

        final var userProfile = new UserProfile(watched.getUniqueId(), watched.getName(), skin);

        PacketWrapper<?> wrapper;
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_19_3)) {
            final var playerInfo = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(userProfile, true, 50,
                                                                                    GameMode.SURVIVAL,
                                                                                    Component.text(watched.getDisplayName()),
                                                                                    null);

            wrapper = new WrapperPlayServerPlayerInfoUpdate(
                    EnumSet.of(WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER, WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED),
                    playerInfo);
        } else {
            final var playerData = new WrapperPlayServerPlayerInfo.PlayerData(Component.text(watched.getDisplayName()), userProfile, GameMode.SURVIVAL, 50);
            wrapper = new WrapperPlayServerPlayerInfo(WrapperPlayServerPlayerInfo.Action.ADD_PLAYER, playerData);
        }

        PacketEvents.getAPI().getPlayerManager().sendPacket(observer, wrapper);
    }

    /**
     * This method is used to act before the hiding {@link com.google.common.collect.Multimap} is updated.
     * An example use case would be to send a packet that is required to hide the entity, but the packet would be blocked after updating the map.
     */
    protected void onPreHide(@NotNull Player observer, @NotNull Set<Player> toHide) {}

    protected void onHide(@NotNull Player observer, @NotNull Set<Player> toHide) {}

    protected abstract void onReveal(@NotNull Player observer, @NotNull Set<Player> revealed);
}
