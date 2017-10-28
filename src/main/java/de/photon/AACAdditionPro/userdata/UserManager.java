package de.photon.AACAdditionPro.userdata;

import com.comphenix.protocol.ProtocolLibrary;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class UserManager implements Listener
{
    // Concurrency to tackle some ConcurrentModificationExceptions
    private static final ConcurrentMap<UUID, User> users;

    static
    {
        users = new ConcurrentSkipListMap<>();
        ProtocolLibrary.getProtocolManager().addPacketListener(new BeaconListener());
    }

    public static User getUser(final UUID uuid)
    {
        return users.get(uuid);
    }

    public static Collection<User> getUsers()
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
        users.remove(event.getPlayer().getUniqueId());
    }
}
