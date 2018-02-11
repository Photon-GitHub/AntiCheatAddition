package de.photon.AACAdditionPro.user;

import com.comphenix.protocol.ProtocolLibrary;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class UserManager implements Listener
{
    // Concurrency to tackle some ConcurrentModificationExceptions
    private static final ConcurrentMap<UUID, User> users = new ConcurrentHashMap<>();

    public UserManager()
    {
        ProtocolLibrary.getProtocolManager().addPacketListener(new BeaconListener());
    }

    public static User getUser(final UUID uuid)
    {
        return users.get(uuid);
    }

    /**
     * Gets all {@link User}s wrapped in a {@link HashSet}. <br>
     * Safe to modify.
     */
    public static Collection<User> getUsers()
    {
        return new HashSet<>(getUsersUnwrapped());
    }

    /**
     * Gets all {@link User}s without wrapping. <br>
     * DO NOT MODIFY THIS COLLECTION; IT WILL MESS UP THE USER MANAGEMENT.
     * <p>
     * Use this solely for performance purposes e.g. in iterations or as a source {@link Collection} for wrapping.
     */
    public static Collection<User> getUsersUnwrapped()
    {
        return users.values();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(final PlayerJoinEvent event)
    {
        users.put(event.getPlayer().getUniqueId(), new User(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event)
    {
        final de.photon.AACAdditionPro.user.User removedUser = users.remove(event.getPlayer().getUniqueId());
        removedUser.unregister();
    }
}
