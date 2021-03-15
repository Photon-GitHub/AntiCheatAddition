package de.photon.aacadditionpro.user;

import com.comphenix.protocol.events.PacketEvent;
import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.InternalPermission;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.val;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User implements CommandSender
{
    private static final ConcurrentMap<UUID, User> USERS = new ConcurrentHashMap<>(1024);
    private static final Set<User> DEBUG_USERS = new CopyOnWriteArraySet<>();

    @Delegate(types = CommandSender.class)
    @EqualsAndHashCode.Include private final Player player;

    /**
     * Creates an {@link User} from a {@link Player}.
     */
    protected static void createFromPlayer(Player player)
    {
        val user = new User(player);
        USERS.put(player.getUniqueId(), user);
        if (InternalPermission.DEBUG.hasPermission(player)) DEBUG_USERS.add(user);
    }

    /**
     * Removes an {@link User}.
     */
    protected static void deleteUser(UUID uuid)
    {
        val removedUser = USERS.remove(uuid);
        DEBUG_USERS.remove(removedUser);
    }

    public static User getUser(UUID uuid)
    {
        return USERS.get(uuid);
    }

    public static User safeGetUserFromPacketEvent(PacketEvent event)
    {
        // Special handling here as a player could potentially log out after this and therefore cause a NPE.
        val player = event.getPlayer();
        return event.isPlayerTemporary() || player == null ? null : getUser(player.getUniqueId());
    }

    /**
     * Gets all {@link User}s without wrapping. <br>
     * DO NOT MODIFY THIS COLLECTION; IT WILL MESS UP THE USER MANAGEMENT.
     * <p>
     * Use this solely for performance purposes e.g. in iterations or as a source {@link Collection} for wrapping.
     */
    public static Collection<User> getUsersUnwrapped()
    {
        return USERS.values();
    }

    public static Set<User> getDebugUsers()
    {
        return DEBUG_USERS;
    }

    /**
     * This checks if this {@link User} still exists and should be checked.
     *
     * @param user             the {@link User} to be checked.
     * @param bypassPermission the bypass permission of the module.
     *
     * @return true if the {@link User} is null or bypassed.
     */
    public static boolean isUserInvalid(@Nullable User user, @NotNull String bypassPermission)
    {
        return user == null || user.getPlayer() == null || user.isBypassed(bypassPermission);
    }

    /**
     * Determines whether a {@link Player} bypasses a certain module.
     */
    public boolean isBypassed(@NotNull String bypassPermission)
    {
        Preconditions.checkArgument(bypassPermission.startsWith(InternalPermission.BYPASS.getRealPermission()), "Invalid bypass permission");
        return InternalPermission.hasPermission(player, bypassPermission);
    }

    /**
     * Gets the debug state (determines whether or not an {@link User} gets debug messages).
     */
    public boolean hasDebug()
    {
        return DEBUG_USERS.contains(this);
    }

    /**
     * Sets the debug state (determines whether or not an {@link User} gets debug messages).
     */
    public void setDebug(boolean debug)
    {
        if (debug) {
            DEBUG_USERS.add(this);
        } else {
            DEBUG_USERS.remove(this);
        }
    }

    public static class UserListener implements Listener
    {
        @EventHandler(priority = EventPriority.LOWEST)
        public void onJoin(final PlayerJoinEvent event)
        {
            createFromPlayer(event.getPlayer());
        }

        @EventHandler
        public void onQuit(final PlayerQuitEvent event)
        {
            deleteUser(event.getPlayer().getUniqueId());
        }
    }
}
