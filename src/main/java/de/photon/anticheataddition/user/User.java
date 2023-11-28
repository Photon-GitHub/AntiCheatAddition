package de.photon.anticheataddition.user;

import com.comphenix.protocol.events.PacketEvent;
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
import de.photon.anticheataddition.util.datastructure.buffer.RingBuffer;
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

    @Delegate(types = Permissible.class)
    @EqualsAndHashCode.Include
    private final Player player;

    private final UserData userData;

    private final ServerVersion clientVersion;

    private final InventoryBatch inventoryBatch = new InventoryBatch(this);
    private final ScaffoldBatch scaffoldBatch = new ScaffoldBatch(this);
    private final TowerBatch towerBatch = new TowerBatch(this);

    public User(Player player) {
        this.player = player;
        final var viaAPI = AntiCheatAddition.getInstance().getViaAPI();
        this.clientVersion = viaAPI == null ? ServerVersion.ACTIVE :
                ServerVersion.getByProtocolVersionNumber(viaAPI.getPlayerVersion(this.player.getUniqueId()))
                        .orElse(ServerVersion.ACTIVE);

        USERS.put(player.getUniqueId(), this);

        final var floodgateApi = AntiCheatAddition.getInstance().getFloodgateApi();
        if (floodgateApi != null && floodgateApi.isFloodgateId(player.getUniqueId())) {
            FLOODGATE_USERS.add(this);
        }

        if (InternalPermission.DEBUG.hasPermission(player)) DEBUG_USERS.add(this);

        this.userData = new UserData();
    }

    // Other methods remain unchanged

    @SuppressWarnings("MethodMayBeStatic")
    public static final class UserListener implements Listener {
        @EventHandler(priority = EventPriority.LOWEST)
        public void onJoin(final PlayerJoinEvent event) {
            final User user = new User(event.getPlayer());

            // Login time
            user.userData.getTimeMap().at(TimeKey.LOGIN_TIME).update();
            // Login should count as movement.
            user.userData.getTimeMap().at(TimeKey.HEAD_OR_OTHER_MOVEMENT).update();
            user.userData.getTimeMap().at(TimeKey.XYZ_MOVEMENT).update();
            user.userData.getTimeMap().at(TimeKey.XZ_MOVEMENT).update();
        }

        @EventHandler
        public void onQuit(final PlayerQuitEvent event) {
            final User oldUser = USERS.remove(event.getPlayer().getUniqueId());
            FLOODGATE_USERS.remove(oldUser);
            DEBUG_USERS.remove(oldUser);
        }
    }
    /**
     * Gets all {@link User}s without wrapping. <br>
     * DO NOT MODIFY THIS COLLECTION; IT WILL MESS UP THE USER MANAGEMENT.
     * <p>
     * Use this solely for performance purposes e.g. in iterations or as a source {@link Collection} for wrapping.
     */
    public static Collection<User> getUsersUnwrapped() {
        return USERS.values();
    }

    public static Set<User> getDebugUsers() {
        return DEBUG_USERS;
    }

    public static User getUser(Player player) {
        return USERS.get(player.getUniqueId());
    }

    public static User getUser(UUID uuid) {
        return USERS.get(uuid);
    }

    @Nullable
    public static User safeGetUserFromPacketEvent(PacketEvent event) {
        // Special handling here as a player could potentially log out after this and therefore cause a NPE.
        if (event.isCancelled() || event.isPlayerTemporary()) return null;
        final Player player = event.getPlayer();
        return player == null ? null : getUser(player);
    }

    public static boolean inAdventureOrSurvivalMode(Player player) {
        return switch (player.getGameMode()) {
            case ADVENTURE, SURVIVAL -> true;
            default -> false;
        };
    }

// Other methods remain unchanged

// Inventory

    public boolean hasOpenInventory() {
        return userData.getTimeMap().at(TimeKey.INVENTORY_OPENED).getTime() != 0;
    }

    public boolean notRecentlyOpenedInventory(final long milliseconds) {
        return userData.getTimeMap().at(TimeKey.INVENTORY_OPENED).notRecentlyUpdated(milliseconds);
    }

    public boolean hasClickedInventoryRecently(final long milliseconds) {
        return userData.getTimeMap().at(TimeKey.INVENTORY_CLICK).recentlyUpdated(milliseconds);
    }

// Movement

    public boolean hasMovedRecently(final TimeKey movementType, final long milliseconds) {
        return switch (movementType) {
            case HEAD_OR_OTHER_MOVEMENT, XYZ_MOVEMENT, XZ_MOVEMENT -> userData.getTimeMap().at(movementType).recentlyUpdated(milliseconds);
            default -> throw new IllegalStateException("Unexpected MovementType: " + movementType);
        };
    }

    public boolean hasSprintedRecently(final long milliseconds) {
        return userData.getData().bool.sprinting || userData.getTimeMap().at(TimeKey.SPRINT_TOGGLE).recentlyUpdated(milliseconds);
    }

    public boolean hasSneakedRecently(final long milliseconds) {
        return userData.getData().bool.sneaking || userData.getTimeMap().at(TimeKey.SNEAK_TOGGLE).recentlyUpdated(milliseconds);
    }

    public boolean hasJumpedRecently(final long milliseconds) {
        return userData.getTimeMap().at(TimeKey.VELOCITY_CHANGE_NO_EXTERNAL_CAUSES).recentlyUpdated(milliseconds);
    }

// Convenience methods for much used timestamps

    public boolean hasTeleportedRecently(final long milliseconds) {
        return userData.getTimeMap().at(TimeKey.TELEPORT).recentlyUpdated(milliseconds);
    }

    public boolean hasChangedWorldsRecently(final long milliseconds) {
        return userData.getTimeMap().at(TimeKey.WORLD_CHANGE).recentlyUpdated(milliseconds);
    }

// Skin

    public boolean updateSkinComponents(int newSkinComponents) {
        final OptionalInt oldSkin = userData.getData().object.skinComponents;
        final boolean result = oldSkin.isPresent() && oldSkin.getAsInt() == newSkinComponents;

        // Update the skin components.
        userData.getData().object.skinComponents = OptionalInt.of(newSkinComponents);
        return result;
    }

    public boolean hasDebug() {
        return DEBUG_USERS.contains(this);
    }

    public void setDebug(boolean debug) {
        if (debug) DEBUG_USERS.add(this);
        else DEBUG_USERS.remove(this);
    }
}
