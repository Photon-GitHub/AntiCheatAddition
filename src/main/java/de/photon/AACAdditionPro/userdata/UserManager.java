package de.photon.AACAdditionPro.userdata;

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
    public UserManager()
    {
        ProtocolLibrary.getProtocolManager().addPacketListener(new BeaconListener());
    }

    // Concurrency to tackle some ConcurrentModificationExceptions
    private static final ConcurrentMap<UUID, User> users = new ConcurrentHashMap<>();

    public static User getUser(final UUID uuid)
    {
        return users.get(uuid);
    }

    public static Collection<User> getUsers()
    {
        return new HashSet<>(users.values());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(final PlayerJoinEvent event)
    {
        users.put(event.getPlayer().getUniqueId(), new User(event.getPlayer()));
        System.out.println("--DEBUG--");
        System.out.println("PlayerName: " + event.getPlayer().getName());
        System.out.println("List size: " + getUsers().size());
        System.out.println("Get: " + getUser(event.getPlayer().getUniqueId()));
        System.out.println("User object: " + new User(event.getPlayer()));

        System.out.println("Pointer:" + ((Object) UserManager.users).toString());
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event)
    {
        System.out.println("--DEBUGQUIT--");
        System.out.println("PlayerName: " + event.getPlayer().getName());
        users.remove(event.getPlayer().getUniqueId());

        System.out.println("Pointer:" + ((Object) UserManager.users).toString());
    }
}
