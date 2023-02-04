package de.photon.anticheataddition.util.visibility;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.util.datastructure.SetUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public abstract class PlayerInformationHider implements Listener
{
    private final SetMultimap<Player, Player> hiddenFromPlayerMap;

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
