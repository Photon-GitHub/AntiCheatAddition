package de.photon.aacadditionpro.user;

import com.comphenix.protocol.events.PacketEvent;
import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.InternalPermission;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.user.data.DataKey;
import de.photon.aacadditionpro.user.data.DataMap;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.user.data.TimestampMap;
import de.photon.aacadditionpro.util.mathematics.Hitbox;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.val;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User implements CommandSender
{
    private static final ConcurrentMap<UUID, User> USERS = new ConcurrentHashMap<>(1024);
    private static final Set<User> DEBUG_USERS = new CopyOnWriteArraySet<>();

    @Delegate(types = CommandSender.class)
    @EqualsAndHashCode.Include private final Player player;

    private final DataMap dataMap = new DataMap();
    private final TimestampMap timestampMap = new TimestampMap();

    /**
     * Creates an {@link User} from a {@link Player}.
     */
    protected static User createFromPlayer(Player player)
    {
        val user = new User(player);
        USERS.put(player.getUniqueId(), user);
        if (InternalPermission.DEBUG.hasPermission(player)) DEBUG_USERS.add(user);
        return user;
    }

    /**
     * Removes an {@link User}.
     */
    protected static void deleteUser(UUID uuid)
    {
        DEBUG_USERS.remove(USERS.remove(uuid));
    }

    public static User getUser(UUID uuid)
    {
        return USERS.get(uuid);
    }

    @Nullable
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
     * @param user   the {@link User} to be checked.
     * @param module the module which bypass permission shall be used.
     *
     * @return true if the {@link User} is null or bypassed.
     */
    public static boolean isUserInvalid(@Nullable User user, @NotNull Module module)
    {
        return user == null || user.getPlayer() == null || user.isBypassed(InternalPermission.bypassPermissionOf(module.getModuleId()));
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
     * Checks if the {@link User} is in {@link GameMode#ADVENTURE} or {@link GameMode#SURVIVAL}
     */
    public boolean inAdventureOrSurvivalMode()
    {
        return this.player.getGameMode() == GameMode.ADVENTURE || this.player.getGameMode() == GameMode.SURVIVAL;
    }

    /**
     * This determines and returnes the correct {@link Hitbox} for this {@link User}.
     *
     * @return {@link Hitbox#SNEAKING_PLAYER} or {@link Hitbox#PLAYER}.
     */
    public Hitbox getHitbox()
    {
        return this.player.isSneaking() ?
               Hitbox.SNEAKING_PLAYER :
               Hitbox.PLAYER;
    }


    // Inventory

    /**
     * Determines whether the {@link User} has a currently opened {@link Inventory} according to
     * {@link AACAdditionPro}s internal data.
     */
    public boolean hasOpenInventory()
    {
        return this.getTimestampMap().at(TimestampKey.INVENTORY_OPENED).getTime() != 0;
    }

    /**
     * Determines if this {@link User} has not had an open inventory for some amount of time.
     *
     * @param milliseconds the amount of time in milliseconds that the {@link User} should not have interacted with an
     *                     {@link Inventory}.
     */
    public boolean notRecentlyOpenedInventory(final long milliseconds)
    {
        return !this.getTimestampMap().at(TimestampKey.INVENTORY_OPENED).recentlyUpdated(milliseconds);
    }

    /**
     * Determines if this {@link User} has recently clicked in an {@link Inventory}.
     *
     * @param milliseconds the amount of time in milliseconds in which the {@link User} should be checked for interactions with an {@link Inventory}.
     */
    public boolean hasClickedInventoryRecently(final long milliseconds)
    {
        return this.getTimestampMap().at(TimestampKey.LAST_INVENTORY_CLICK).recentlyUpdated(milliseconds);
    }


    // Movement

    /**
     * Checks if this {@link User} has moved recently.
     *
     * @param movementType what movement should be checked
     * @param milliseconds the amount of time in milliseconds that should be considered.
     *
     * @return true if the player has moved in the specified time frame.
     */
    public boolean hasMovedRecently(final TimestampKey movementType, final long milliseconds)
    {
        switch (movementType) {
            case LAST_HEAD_OR_OTHER_MOVEMENT:
            case LAST_XYZ_MOVEMENT:
            case LAST_XZ_MOVEMENT:
                return this.timestampMap.at(movementType).recentlyUpdated(milliseconds);
            default:
                throw new IllegalStateException("Unexpected MovementType: " + movementType);
        }
    }

    /**
     * Checks if this {@link User} has sprinted recently
     *
     * @param milliseconds the amount of time in milliseconds that should be considered.
     *
     * @return true if the player has sprinted in the specified time frame.
     */
    public boolean hasSprintedRecently(final long milliseconds)
    {
        return this.dataMap.getBoolean(DataKey.BooleanKey.SPRINTING) || this.timestampMap.at(TimestampKey.LAST_SPRINT_TOGGLE).recentlyUpdated(milliseconds);
    }

    /**
     * Checks if this {@link User} has sneaked recently
     *
     * @param milliseconds the amount of time in milliseconds that should be considered.
     *
     * @return true if the player has sneaked in the specified time frame.
     */
    public boolean hasSneakedRecently(final long milliseconds)
    {
        return this.dataMap.getBoolean(DataKey.BooleanKey.SNEAKING) || this.timestampMap.at(TimestampKey.LAST_SNEAK_TOGGLE).recentlyUpdated(milliseconds);
    }


    // Convenience methods for much used timestamps

    /**
     * Determines if this {@link User} has recently teleported.
     * This includes ender pearls as well as respawns, world changes and ordinary teleports.
     *
     * @param milliseconds the amount of time in milliseconds that should be considered.
     */
    public boolean hasTeleportedRecently(final long milliseconds)
    {
        return this.timestampMap.at(TimestampKey.LAST_TELEPORT).recentlyUpdated(milliseconds);
    }

    /**
     * Determines if this {@link User} has recently respawned.
     *
     * @param milliseconds the amount of time in milliseconds that should be considered.
     */
    public boolean hasRespawnedRecently(final long milliseconds)
    {
        return this.timestampMap.at(TimestampKey.LAST_RESPAWN).recentlyUpdated(milliseconds);
    }

    /**
     * Determines if this {@link User} has recently changed worlds.
     *
     * @param milliseconds the amount of time in milliseconds that should be considered.
     */
    public boolean hasChangedWorldsRecently(final long milliseconds)
    {
        return this.timestampMap.at(TimestampKey.LAST_WORLD_CHANGE).recentlyUpdated(milliseconds);
    }

    /**
     * Determines if this {@link User} has recently logged in.
     *
     * @param milliseconds the amount of time in milliseconds that should be considered.
     */
    public boolean hasLoggedInRecently(final long milliseconds)
    {
        return this.timestampMap.at(TimestampKey.LOGIN_TIME).recentlyUpdated(milliseconds);
    }


    // Skin

    /**
     * Updates the saved skin components.
     *
     * @return true if the skinComponents changed and there have already been some skin components beforehand.
     */
    public boolean updateSkinComponents(int newSkinComponents)
    {
        val oldSkin = this.getDataMap().getInt(DataKey.IntegerKey.SKIN_COMPONENTS);

        if (oldSkin == null) {
            this.getDataMap().setInt(DataKey.IntegerKey.SKIN_COMPONENTS, newSkinComponents);
            return false;
        }

        if (oldSkin == newSkinComponents) return false;

        this.getDataMap().setInt(DataKey.IntegerKey.SKIN_COMPONENTS, newSkinComponents);
        return true;
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
            val user = createFromPlayer(event.getPlayer());

            // Login time
            user.timestampMap.at(TimestampKey.LOGIN_TIME).update();
            // Login should count as movement.
            user.timestampMap.at(TimestampKey.LAST_HEAD_OR_OTHER_MOVEMENT).update();
            user.timestampMap.at(TimestampKey.LAST_XYZ_MOVEMENT).update();
            user.timestampMap.at(TimestampKey.LAST_XZ_MOVEMENT).update();
        }

        @EventHandler
        public void onQuit(final PlayerQuitEvent event)
        {
            deleteUser(event.getPlayer().getUniqueId());
        }
    }
}
