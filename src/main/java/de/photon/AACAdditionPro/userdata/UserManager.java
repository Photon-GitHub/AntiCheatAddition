package de.photon.AACAdditionPro.userdata;

import com.comphenix.protocol.ProtocolLibrary;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager implements Listener
{
    public UserManager()
    {
        ProtocolLibrary.getProtocolManager().addPacketListener(new BeaconListener());
    }

    // Concurrency to tackle some ConcurrentModificationExceptions
    private static final Set<User> users = ConcurrentHashMap.newKeySet();

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
