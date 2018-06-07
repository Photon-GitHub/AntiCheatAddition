package de.photon.AACAdditionPro.user;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class UserManager implements Listener
{
    // Concurrency to tackle some ConcurrentModificationExceptions
    private static final ConcurrentMap<UUID, User> users = new ConcurrentHashMap<>();

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

    /**
     * Gets all {@link User}s that have activated verbose.
     */
    public static Collection<User> getVerboseUsers()
    {
        final List<User> verboseUsers = new ArrayList<>();
        for (User user : getUsersUnwrapped())
        {
            if (user.verbose)
            {
                verboseUsers.add(user);
            }
        }
        return verboseUsers;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(final PlayerJoinEvent event)
    {
        users.put(event.getPlayer().getUniqueId(), new User(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event)
    {
        final User removedUser = users.remove(event.getPlayer().getUniqueId());
        removedUser.unregister();
    }
}
