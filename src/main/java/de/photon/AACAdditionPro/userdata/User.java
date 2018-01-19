package de.photon.AACAdditionPro.userdata;

import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.userdata.data.AutoPotionData;
import de.photon.AACAdditionPro.userdata.data.ClientSideEntityData;
import de.photon.AACAdditionPro.userdata.data.ElytraData;
import de.photon.AACAdditionPro.userdata.data.EspInformationData;
import de.photon.AACAdditionPro.userdata.data.FishingData;
import de.photon.AACAdditionPro.userdata.data.InventoryData;
import de.photon.AACAdditionPro.userdata.data.InventoryHeuristicsData;
import de.photon.AACAdditionPro.userdata.data.LookPacketData;
import de.photon.AACAdditionPro.userdata.data.PingData;
import de.photon.AACAdditionPro.userdata.data.PositionData;
import de.photon.AACAdditionPro.userdata.data.PotionData;
import de.photon.AACAdditionPro.userdata.data.ScaffoldData;
import de.photon.AACAdditionPro.userdata.data.SettingsData;
import de.photon.AACAdditionPro.userdata.data.TeamingData;
import de.photon.AACAdditionPro.userdata.data.TeleportData;
import de.photon.AACAdditionPro.userdata.data.TimeData;
import de.photon.AACAdditionPro.userdata.data.TowerData;
import de.photon.AACAdditionPro.userdata.data.VelocityChangeData;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.UUID;

@Getter
public class User
{
    private final Player player;

    private final AutoPotionData autoPotionData = new AutoPotionData(this);
    private final ClientSideEntityData clientSideEntityData = new ClientSideEntityData(this);
    private final ElytraData elytraData = new ElytraData(this);
    private final EspInformationData espInformationData = new EspInformationData(this);
    private final TimeData fastSwitchData = new TimeData(false, this);
    private final FishingData fishingData = new FishingData(this);
    private final InventoryData inventoryData = new InventoryData(this);
    private final InventoryHeuristicsData inventoryHeuristicsData = new InventoryHeuristicsData();
    private final LookPacketData lookPacketData = new LookPacketData(this);
    private final TimeData loginData;
    private final PingData pingData = new PingData(this);
    private final PositionData positionData = new PositionData(this);
    private final PotionData potionData = new PotionData(this);
    private final ScaffoldData scaffoldData = new ScaffoldData(this);
    private final SettingsData settingsData = new SettingsData();
    private final TeamingData teamingData = new TeamingData(this);
    private final TeleportData teleportData = new TeleportData(this);
    private final TowerData towerData = new TowerData(this);
    private final VelocityChangeData velocityChangeData = new VelocityChangeData(this);

    /**
     * Should the player receive verbose-messages?
     * This can be set via the /aacadditionpro verbose command
     */
    public boolean verbose;

    public User(final Player player)
    {
        this.player = player;
        this.loginData = new TimeData(false, this, System.currentTimeMillis());
        this.verbose = InternalPermission.hasPermission(player, InternalPermission.VERBOSE);
    }

    public boolean isBypassed()
    {
        return InternalPermission.hasPermission(this.player, InternalPermission.BYPASS);
    }

    /**
     * Used to see if a {@link UUID} refers to this {@link User}
     *
     * @param uuid the uuid of the given {@link Player}
     *
     * @return true if the uuid refers to this {@link User}
     */
    public boolean refersToUUID(final UUID uuid)
    {
        return uuid.equals(this.player.getUniqueId());
    }

    /**
     * @return true if the {@link de.photon.AACAdditionPro.userdata.User} is null or bypassed.
     */
    public static boolean isUserInvalid(User user)
    {
        return user == null || user.isBypassed();
    }
}