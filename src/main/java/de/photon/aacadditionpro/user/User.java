package de.photon.aacadditionpro.user;

import de.photon.aacadditionpro.InternalPermission;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.olduser.UserManager;
import de.photon.aacadditionpro.user.subdata.ConsumeData;
import de.photon.aacadditionpro.user.subdata.FishingData;
import de.photon.aacadditionpro.user.subdata.InventoryData;
import lombok.Getter;
import org.bukkit.entity.Player;

@Getter
public class User
{
    private Player player;
    private LongDataMap<DataKey> dataMap = new LongDataMap<>(DataKey.class, dk -> dk.isTimeStamp);

    private ConsumeData consumeData = new ConsumeData(this);
    private FishingData fishingData = new FishingData(this);
    private InventoryData inventoryData = new InventoryData(this);

    public User(final Player player)
    {
        this.player = player;
        UserManager.setVerbose(this, InternalPermission.AAC_VERBOSE.hasPermission(player));
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
     * This method unregisters the {@link User} to make sure that memory leaks will not happen, and if they do,
     * their impact is very small.
     */
    public void unregister()
    {
        this.player = null;
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
