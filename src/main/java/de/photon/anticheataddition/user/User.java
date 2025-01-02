package de.photon.anticheataddition.user;

import com.github.davidmoten.rtreemulti.Entry;
import com.github.davidmoten.rtreemulti.geometry.Point;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.google.common.base.Preconditions;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.InternalPermission;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.data.Data;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.user.data.TimestampMap;
import de.photon.anticheataddition.user.data.batch.InventoryBatch;
import de.photon.anticheataddition.user.data.batch.ScaffoldBatch;
import de.photon.anticheataddition.user.data.batch.TowerBatch;
import de.photon.anticheataddition.user.data.subdata.BrandChannelData;
import de.photon.anticheataddition.user.data.subdata.LookPacketData;
import de.photon.anticheataddition.util.log.Log;
import de.photon.anticheataddition.util.mathematics.Hitbox;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class User implements Permissible
{
    private static final ConcurrentMap<UUID, User> USERS = new ConcurrentHashMap<>(AntiCheatAddition.SERVER_EXPECTED_PLAYERS);
    private static final Set<User> FLOODGATE_USERS = ConcurrentHashMap.newKeySet(AntiCheatAddition.SERVER_EXPECTED_PLAYERS);
    private static final Set<User> DEBUG_USERS = new CopyOnWriteArraySet<>();

    @Delegate(types = Permissible.class) @EqualsAndHashCode.Include private final Player player;

    private final Data data = new Data();
    private final TimestampMap timeMap = new TimestampMap();

    private final InventoryBatch inventoryBatch = new InventoryBatch(this);
    private final ScaffoldBatch scaffoldBatch = new ScaffoldBatch(this);
    private final TowerBatch towerBatch = new TowerBatch(this);

    private final BrandChannelData brandChannelData = new BrandChannelData();
    private final LookPacketData lookPacketData = new LookPacketData();

    /**
     * This is the minecraft version of the client.
     * It may differ from the active server version e.g. due to ViaVersion.
     */
    private final ServerVersion clientVersion;

    /**
     * Creates an {@link User} from a {@link Player}.
     */
    public User(Player player)
    {
        this.player = player;
        final var viaAPI = AntiCheatAddition.getInstance().getViaAPI();
        this.clientVersion = viaAPI == null ? ServerVersion.ACTIVE : ServerVersion.getByProtocolVersionNumber(viaAPI.getPlayerVersion(this.player.getUniqueId())).orElse(ServerVersion.ACTIVE);

        USERS.put(player.getUniqueId(), this);

        final var floodgateApi = AntiCheatAddition.getInstance().getFloodgateApi();
        if (floodgateApi != null && floodgateApi.isFloodgatePlayer(player.getUniqueId())) {
            FLOODGATE_USERS.add(this);
        }

        if (InternalPermission.DEBUG.hasPermission(player)) DEBUG_USERS.add(this);

        // Join log for debugging purposes.
        Log.finer(() -> "User %s created | General bypass permissions: %s | Debug permissions: %s".formatted(player.getName(), InternalPermission.BYPASS.hasPermission(player), InternalPermission.DEBUG.hasPermission(player)));
    }

    public static User getUser(PacketReceiveEvent event)
    {
        final var player = event.getPlayer();
        return player == null ? null : getUser((Player) player);
    }

    public static User getUser(PacketSendEvent event)
    {
        return getUser((Player) event.getPlayer());
    }

    public static User getUser(Player player)
    {
        return USERS.get(player.getUniqueId());
    }

    public static User getUser(UUID uuid)
    {
        return USERS.get(uuid);
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
        return isUserInvalid(user, module.getBypassPermission());
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
        return user == null || user.getPlayer() == null || user.isFloodgate() || user.isBypassed(bypassPermission);
    }

    /**
     * Checks if the {@link Player} is in {@link GameMode#ADVENTURE} or {@link GameMode#SURVIVAL}
     */
    public static boolean inAdventureOrSurvivalMode(Player player)
    {
        return switch (player.getGameMode()) {
            case ADVENTURE, SURVIVAL -> true;
            default -> false;
        };
    }

    /**
     * Creates an {@link Entry} of the {@link Player} and their location.
     * This is a convenience method for RTree operations.
     */
    public static Entry<Player, Point> rTreeEntryFromPlayer(Player player)
    {
        final var location = player.getLocation();
        return Entry.entry(player, Point.create(location.getX(), location.getY(), location.getZ()));
    }

    /**
     * Determines whether a {@link Player} bypasses a certain module.
     */
    public boolean isBypassed(@NotNull String bypassPermission)
    {
        Preconditions.checkArgument(bypassPermission.startsWith(InternalPermission.BYPASS.getRealPermission()), "Invalid bypass permission");
        return InternalPermission.hasPermission(player, bypassPermission);
    }

    public boolean isFloodgate()
    {
        return FLOODGATE_USERS.contains(this);
    }

    /**
     * Checks if the {@link User} is in {@link GameMode#ADVENTURE} or {@link GameMode#SURVIVAL}
     */
    public boolean inAdventureOrSurvivalMode()
    {
        return inAdventureOrSurvivalMode(this.player);
    }

    /**
     * This determines and returns the correct {@link Hitbox} for this {@link User}.
     *
     * @return {@link Hitbox#SNEAKING_PLAYER} or {@link Hitbox#PLAYER}.
     */
    public Hitbox.HitboxLocation getHitboxLocation()
    {
        return Hitbox.hitboxLocationOf(this.player);
    }

    /**
     * Checks if the {@link User} is currently in any liquid.
     */
    public boolean isInLiquids()
    {
        return this.getHitboxLocation().isInLiquids();
    }


    // Inventory

    /**
     * Determines whether the {@link User} has a currently opened {@link Inventory} according to
     * {@link AntiCheatAddition}s internal data.
     */
    public boolean hasOpenInventory()
    {
        return this.getTimeMap().at(TimeKey.INVENTORY_OPENED).getTime() != 0;
    }

    /**
     * Determines if this {@link User} has not had an open inventory for some amount of time.
     *
     * @param milliseconds the amount of time in milliseconds that the {@link User} should not have interacted with an
     *                     {@link Inventory}.
     */
    public boolean notRecentlyOpenedInventory(final long milliseconds)
    {
        return this.getTimeMap().at(TimeKey.INVENTORY_OPENED).notRecentlyUpdated(milliseconds);
    }

    /**
     * Determines if this {@link User} has recently clicked in an {@link Inventory}.
     *
     * @param milliseconds the amount of time in milliseconds in which the {@link User} should be checked for interactions with an {@link Inventory}.
     */
    public boolean hasClickedInventoryRecently(final long milliseconds)
    {
        return this.getTimeMap().at(TimeKey.INVENTORY_CLICK).recentlyUpdated(milliseconds);
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
    public boolean hasMovedRecently(final TimeKey movementType, final long milliseconds)
    {
        return switch (movementType) {
            case HEAD_OR_OTHER_MOVEMENT, XYZ_MOVEMENT, XZ_MOVEMENT -> this.timeMap.at(movementType).recentlyUpdated(milliseconds);
            default -> throw new IllegalStateException("Unexpected MovementType: " + movementType);
        };
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
        return this.data.bool.sprinting || this.timeMap.at(TimeKey.SPRINT_TOGGLE).recentlyUpdated(milliseconds);
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
        return this.data.bool.sneaking || this.timeMap.at(TimeKey.SNEAK_TOGGLE).recentlyUpdated(milliseconds);
    }

    /**
     * Checks if this {@link User} has jumped recently
     *
     * @param milliseconds the amount of time in milliseconds that should be considered.
     *
     * @return true if the player has sneaked in the specified time frame.
     */
    public boolean hasJumpedRecently(final long milliseconds)
    {
        return this.timeMap.at(TimeKey.VELOCITY_CHANGE_NO_EXTERNAL_CAUSES).recentlyUpdated(milliseconds);
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
        return this.timeMap.at(TimeKey.TELEPORT).recentlyUpdated(milliseconds);
    }

    /**
     * Determines if this {@link User} has recently changed worlds.
     *
     * @param milliseconds the amount of time in milliseconds that should be considered.
     */
    public boolean hasChangedWorldsRecently(final long milliseconds)
    {
        return this.timeMap.at(TimeKey.WORLD_CHANGE).recentlyUpdated(milliseconds);
    }

    /**
     * Creates an {@link Entry} of this {@link User} and their location.
     * This is a convenience method for RTree operations.
     */
    public Entry<Player, Point> rTreeEntry()
    {
        return rTreeEntryFromPlayer(this.player);
    }

    // Skin

    /**
     * Updates the saved skin components.
     *
     * @return true if the skinComponents changed and there have already been some skin components beforehand.
     */
    public boolean updateSkinComponents(int newSkinComponents)
    {
        final OptionalInt oldSkin = this.data.object.skinComponents;
        final boolean result = oldSkin.isPresent() && oldSkin.getAsInt() == newSkinComponents;

        // Update the skin components.
        this.data.object.skinComponents = OptionalInt.of(newSkinComponents);
        return result;
    }


    /**
     * Gets the debug state (determines whether an {@link User} gets debug messages).
     */
    public boolean hasDebug()
    {
        return DEBUG_USERS.contains(this);
    }

    /**
     * Sets the debug state (determines whether an {@link User} gets debug messages).
     */
    public void setDebug(boolean debug)
    {
        if (debug) DEBUG_USERS.add(this);
        else DEBUG_USERS.remove(this);
    }

    @SuppressWarnings("MethodMayBeStatic")
    public static final class UserListener implements Listener
    {
        @EventHandler(priority = EventPriority.LOWEST)
        public void onJoin(final PlayerJoinEvent event)
        {
            final User user = new User(event.getPlayer());

            // Login time
            user.timeMap.at(TimeKey.LOGIN_TIME).update();
            // Login should count as movement.
            user.timeMap.at(TimeKey.HEAD_OR_OTHER_MOVEMENT).update();
            user.timeMap.at(TimeKey.XYZ_MOVEMENT).update();
            user.timeMap.at(TimeKey.XZ_MOVEMENT).update();
        }

        @EventHandler
        public void onQuit(final PlayerQuitEvent event)
        {
            final User oldUser = USERS.remove(event.getPlayer().getUniqueId());
            FLOODGATE_USERS.remove(oldUser);
            DEBUG_USERS.remove(oldUser);
        }
    }
}
