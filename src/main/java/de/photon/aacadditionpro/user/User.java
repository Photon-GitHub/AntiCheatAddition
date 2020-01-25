package de.photon.aacadditionpro.user;

import de.photon.aacadditionpro.InternalPermission;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.user.data.AutoPotionData;
import de.photon.aacadditionpro.user.data.ConsumeData;
import de.photon.aacadditionpro.user.data.FishingData;
import de.photon.aacadditionpro.user.data.InventoryData;
import de.photon.aacadditionpro.user.data.KeepAliveData;
import de.photon.aacadditionpro.user.data.LookPacketData;
import de.photon.aacadditionpro.user.data.PacketAnalysisData;
import de.photon.aacadditionpro.user.data.PingData;
import de.photon.aacadditionpro.user.data.PositionData;
import de.photon.aacadditionpro.user.data.ScaffoldData;
import de.photon.aacadditionpro.user.data.SkinData;
import de.photon.aacadditionpro.user.data.TeamingData;
import de.photon.aacadditionpro.user.data.TeleportData;
import de.photon.aacadditionpro.user.data.TowerData;
import de.photon.aacadditionpro.user.data.VelocityChangeData;
import lombok.Getter;
import org.bukkit.entity.Player;

@Getter
public class User
{
    private Player player;

    private final AutoPotionData autoPotionData = new AutoPotionData(this);
    private final ConsumeData consumeData = new ConsumeData(this);
    private final TimeData fastSwitchData = new TimeData(this);
    private final FishingData fishingData = new FishingData(this);
    private final InventoryData inventoryData = new InventoryData(this);
    private final KeepAliveData keepAliveData = new KeepAliveData(this);
    private final TimeData loginData;
    private final LookPacketData lookPacketData = new LookPacketData(this);
    private final PacketAnalysisData packetAnalysisData = new PacketAnalysisData(this);
    private final PingData pingData = new PingData(this);
    private final PositionData positionData = new PositionData(this);
    private final ScaffoldData scaffoldData = new ScaffoldData(this);
    private SkinData skinData = new SkinData();
    private final TeamingData teamingData = new TeamingData(this);
    private final TeleportData teleportData = new TeleportData(this);
    private final TowerData towerData = new TowerData(this);
    private final VelocityChangeData velocityChangeData = new VelocityChangeData(this);

    public User(final Player player)
    {
        this.player = player;
        this.loginData = new TimeData(this, System.currentTimeMillis());
        UserManager.setVerbose(this, InternalPermission.VERBOSE.hasPermission(player));
    }

    public boolean isBypassed(ModuleType moduleType)
    {
        return InternalPermission.hasPermission(this.player, InternalPermission.BYPASS.getRealPermission() + '.' + moduleType.getConfigString().toLowerCase());
    }

    /**
     * @return true if the {@link User} is null or bypassed.
     */
    public static boolean isUserInvalid(final User user, final ModuleType moduleType)
    {
        return user == null || user.isBypassed(moduleType) || user.getPlayer() == null;
    }

    void unregister()
    {
        this.player = null;
        autoPotionData.unregister();
        consumeData.unregister();
        fastSwitchData.unregister();
        fishingData.unregister();
        inventoryData.unregister();
        keepAliveData.unregister();
        loginData.unregister();
        lookPacketData.unregister();
        packetAnalysisData.unregister();
        pingData.unregister();
        positionData.unregister();
        scaffoldData.unregister();
        skinData = null;
        teamingData.unregister();
        teleportData.unregister();
        towerData.unregister();
        velocityChangeData.unregister();
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
