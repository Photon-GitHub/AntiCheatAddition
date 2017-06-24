package de.photon.AACAdditionPro.userdata;

import com.comphenix.protocol.ProtocolLibrary;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

@SuppressWarnings("MethodMayBeStatic")
public class UserManager implements Listener
{
    public UserManager()
    {
        ProtocolLibrary.getProtocolManager().addPacketListener(new BeaconListener());
    }

    private static final SortedSet<User> users = Collections.synchronizedSortedSet(new TreeSet<>());

    public static synchronized User getUser(final UUID uuid)
    {
        for (final User user : users) {
            if (user.refersToUUID(uuid)) {
                return user;
            }
        }
        return null;
    }

    public static Set<User> getUsers()
    {
        return users;
    }

    @EventHandler
    public void on(final PlayerJoinEvent event)
    {
        users.add(new User(event.getPlayer()));
    }

    @EventHandler
    public void on(final PlayerQuitEvent event)
    {
        users.remove(getUser(event.getPlayer().getUniqueId()));
    }
}
