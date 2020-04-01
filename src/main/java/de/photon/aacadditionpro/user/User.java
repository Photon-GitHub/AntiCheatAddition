package de.photon.aacadditionpro.user;

import de.photon.aacadditionpro.InternalPermission;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.user.subdata.FishingData;
import de.photon.aacadditionpro.user.subdata.KeepAliveData;
import de.photon.aacadditionpro.user.subdata.LookPacketData;
import lombok.Getter;
import org.bukkit.entity.Player;

@Getter
public class User
{
    private Player player;
    private TimestampMap<TimestampKey> timestampMap;
    private ObjectDataMap<DataKey> dataMap;

    private FishingData fishingData = new FishingData(this);
    private KeepAliveData keepAliveData = new KeepAliveData(this);
    private LookPacketData lookPacketData = new LookPacketData(this);

    public User(final Player player)
    {
        this.player = player;
        UserManager.setVerbose(this, InternalPermission.AAC_VERBOSE.hasPermission(player));

        // Timestamps
        this.timestampMap = new TimestampMap<>(TimestampKey.class);
        this.timestampMap.nullifyTimeStamps(TimestampKey.values());

        // Login time
        this.getTimestampMap().updateTimeStamp(TimestampKey.LOGIN_TIME);
        // Login should count as movement.
        this.getTimestampMap().updateTimeStamp(TimestampKey.LAST_HEAD_OR_OTHER_MOVEMENT);
        this.getTimestampMap().updateTimeStamp(TimestampKey.LAST_XYZ_MOVEMENT);
        this.getTimestampMap().updateTimeStamp(TimestampKey.LAST_XZ_MOVEMENT);

        // Data
        this.dataMap = new ObjectDataMap<>(DataKey.class, (key, value) -> key.getClazz().isAssignableFrom(value.getClass()));
        for (DataKey value : DataKey.values()) {
            this.dataMap.setValue(value, value.getDefaultValue());
        }
    }


    // Basics

    /**
     * This checks if this {@link User} still exists and should be checked.
     *
     * @param user       the {@link User} to be checked.
     * @param moduleType the {@link ModuleType} that should be used to determine if the {@link User} is bypassed.
     *
     * @return true if the {@link User} is null or bypassed.
     */
    public static boolean isUserInvalid(final User user, final ModuleType moduleType)
    {
        return user == null || user.getPlayer() == null || user.isBypassed(moduleType);
    }

    /**
     * Determines whether a {@link User} bypasses a certain {@link ModuleType}.
     */
    public boolean isBypassed(ModuleType moduleType)
    {
        return InternalPermission.hasPermission(this.player, InternalPermission.BYPASS.getRealPermission() + '.' + moduleType.getConfigString().toLowerCase());
    }


    // Inventory

    /**
     * Determines whether the {@link User} has a currently opened {@link org.bukkit.inventory.Inventory} according to
     * {@link de.photon.aacadditionpro.AACAdditionPro}s internal data.
     */
    public boolean hasOpenInventory()
    {
        return this.getTimestampMap().getTimeStamp(TimestampKey.INVENTORY_OPENED) != 0;
    }

    /**
     * Determines if this {@link User} has not had an open inventory for some amount of time.
     *
     * @param milliseconds the amount of time in milliseconds that the {@link User} should not have interacted with an
     *                     {@link org.bukkit.inventory.Inventory}.
     */
    public boolean notRecentlyOpenedInventory(final long milliseconds)
    {
        return !this.getTimestampMap().recentlyUpdated(TimestampKey.INVENTORY_OPENED, milliseconds);
    }

    /**
     * Determines if this {@link User} has recently clicked in an {@link org.bukkit.inventory.Inventory}.
     *
     * @param milliseconds the amount of time in milliseconds in which the {@link User} should be checked for
     *                     interactions with an {@link org.bukkit.inventory.Inventory}.
     */
    public boolean hasClickedInventoryRecently(final long milliseconds)
    {
        return this.getTimestampMap().recentlyUpdated(TimestampKey.LAST_INVENTORY_CLICK, milliseconds);
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
                return this.timestampMap.recentlyUpdated(TimestampKey.LAST_HEAD_OR_OTHER_MOVEMENT, milliseconds);
            case LAST_XYZ_MOVEMENT:
                return this.timestampMap.recentlyUpdated(TimestampKey.LAST_XYZ_MOVEMENT, milliseconds);
            case LAST_XZ_MOVEMENT:
                return this.timestampMap.recentlyUpdated(TimestampKey.LAST_XZ_MOVEMENT, milliseconds);
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
        return this.dataMap.getBoolean(DataKey.SPRINTING) || this.timestampMap.recentlyUpdated(TimestampKey.LAST_SPRINT_TOGGLE, milliseconds);
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
        return this.dataMap.getBoolean(DataKey.SNEAKING) || this.timestampMap.recentlyUpdated(TimestampKey.LAST_SNEAK_TOGGLE, milliseconds);
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
        return this.getTimestampMap().recentlyUpdated(TimestampKey.LAST_TELEPORT, milliseconds);
    }

    /**
     * Determines if this {@link User} has recently respawned.
     *
     * @param milliseconds the amount of time in milliseconds that should be considered.
     */
    public boolean hasRespawnedRecently(final long milliseconds)
    {
        return this.getTimestampMap().recentlyUpdated(TimestampKey.LAST_RESPAWN, milliseconds);
    }

    /**
     * Determines if this {@link User} has recently changed worlds.
     *
     * @param milliseconds the amount of time in milliseconds that should be considered.
     */
    public boolean hasChangedWorldsRecently(final long milliseconds)
    {
        return this.getTimestampMap().recentlyUpdated(TimestampKey.LAST_WORLD_CHANGE, milliseconds);
    }

    /**
     * Determines if this {@link User} has recently logged in.
     *
     * @param milliseconds the amount of time in milliseconds that should be considered.
     */
    public boolean hasLoggedInRecently(final long milliseconds)
    {
        return this.getTimestampMap().recentlyUpdated(TimestampKey.LOGIN_TIME, milliseconds);
    }


    // Skin

    /**
     * Updates the saved skin components.
     *
     * @return true if the skinComponents changed and there have already been some skin components beforehand.
     */
    public boolean updateSkinComponents(int newSkinComponents)
    {
        Integer oldSkin = (Integer) this.getDataMap().getValue(DataKey.SKIN_COMPONENTS);

        if (oldSkin == null) {
            this.getDataMap().setValue(DataKey.SKIN_COMPONENTS, newSkinComponents);
            return false;
        }

        if (oldSkin == newSkinComponents) {
            return false;
        }

        this.getDataMap().setValue(DataKey.SKIN_COMPONENTS, newSkinComponents);
        return true;
    }


    // Disabling, equals() and hashCode()

    /**
     * This method unregisters the {@link User} to make sure that memory leaks will not happen, and if they do,
     * their impact is very small.
     */
    public void unregister()
    {
        this.player = null;
        this.timestampMap.clear();
        this.timestampMap = null;
        this.dataMap.clear();
        this.dataMap = null;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return this.player.getUniqueId().equals(((User) o).player.getUniqueId());
    }

    @Override
    public int hashCode()
    {
        return 47 + (this.player == null ? 0 : player.getUniqueId().hashCode());
    }
}
