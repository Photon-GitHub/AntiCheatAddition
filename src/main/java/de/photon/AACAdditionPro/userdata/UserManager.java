package de.photon.AACAdditionPro.userdata;

import com.comphenix.protocol.ProtocolLibrary;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager implements Listener
{
    public UserManager()
    {
        ProtocolLibrary.getProtocolManager().addPacketListener(new BeaconListener());
    }

    // Concurrency to tackle some ConcurrentModificationExceptions
    private static final ConcurrentHashMap<UUID, User> users = new ConcurrentHashMap<>();

    public static User getUser(final UUID uuid)
    {
        return users.get(uuid);
    }

    public static Collection<User> getUsers()
    {
        return users.values();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void on(final PlayerJoinEvent event)
    {
        users.put(event.getPlayer().getUniqueId(), new User(event.getPlayer()));
    }

    @EventHandler
    public void on(final PlayerQuitEvent event)
    {
        users.remove(event.getPlayer().getUniqueId());
    }
}
