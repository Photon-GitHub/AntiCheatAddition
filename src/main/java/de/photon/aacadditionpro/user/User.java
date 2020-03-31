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
        for (TimestampKey value : TimestampKey.values()) {
            this.timestampMap.nullifyTimeStamp(value);
        }

        // Login time
        this.getTimestampMap().updateTimeStamp(TimestampKey.LOGIN_TIME);

        // Data
        this.dataMap = new ObjectDataMap<>(DataKey.class, (key, value) -> key.getClazz().isAssignableFrom(value.getClass()));
        for (DataKey value : DataKey.values()) {
            this.dataMap.setValue(value, value.getDefaultValue());
        }
    }

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
    public boolean recentlyClickedInventory(final long milliseconds)
    {
        return this.getTimestampMap().recentlyUpdated(TimestampKey.LAST_INVENTORY_CLICK, milliseconds);
    }

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
