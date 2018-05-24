package de.photon.AACAdditionPro.user;

import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.user.data.AutoPotionData;
import de.photon.AACAdditionPro.user.data.ClientSideEntityData;
import de.photon.AACAdditionPro.user.data.EspInformationData;
import de.photon.AACAdditionPro.user.data.FishingData;
import de.photon.AACAdditionPro.user.data.InventoryData;
import de.photon.AACAdditionPro.user.data.InventoryHeuristicsData;
import de.photon.AACAdditionPro.user.data.LookPacketData;
import de.photon.AACAdditionPro.user.data.PingData;
import de.photon.AACAdditionPro.user.data.PositionData;
import de.photon.AACAdditionPro.user.data.ScaffoldData;
import de.photon.AACAdditionPro.user.data.SkinData;
import de.photon.AACAdditionPro.user.data.TeamingData;
import de.photon.AACAdditionPro.user.data.TeleportData;
import de.photon.AACAdditionPro.user.data.TowerData;
import de.photon.AACAdditionPro.user.data.VelocityChangeData;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.UUID;

@Getter
public class User
{
    private Player player;

    private AutoPotionData autoPotionData = new AutoPotionData(this);
    private ClientSideEntityData clientSideEntityData = new ClientSideEntityData(this);
    private EspInformationData espInformationData = new EspInformationData(this);
    private TimeData fastSwitchData = new TimeData(this);
    private FishingData fishingData = new FishingData(this);
    private InventoryData inventoryData = new InventoryData(this);
    private InventoryHeuristicsData inventoryHeuristicsData = new InventoryHeuristicsData(this);
    private TimeData loginData;
    private LookPacketData lookPacketData = new LookPacketData(this);
    private PingData pingData = new PingData(this);
    private PositionData positionData = new PositionData(this);
    private ScaffoldData scaffoldData = new ScaffoldData(this);
    private SkinData skinData = new SkinData();
    private TeamingData teamingData = new TeamingData(this);
    private TeleportData teleportData = new TeleportData(this);
    private TowerData towerData = new TowerData(this);
    private VelocityChangeData velocityChangeData = new VelocityChangeData(this);

    /**
     * Should the player receive verbose-messages?
     * This can be set via the /aacadditionpro verbose command
     */
    public boolean verbose;

    public User(final Player player)
    {
        this.player = player;
        this.loginData = new TimeData(this, System.currentTimeMillis());
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
     * @return true if the {@link User} is null or bypassed.
     */
    public static boolean isUserInvalid(final User user)
    {
        return user == null || user.isBypassed();
    }

    void unregister()
    {
        this.player = null;

        autoPotionData.unregister();
        autoPotionData = null;

        clientSideEntityData.unregister();
        clientSideEntityData = null;

        espInformationData.unregister();
        espInformationData = null;

        fastSwitchData.unregister();
        fastSwitchData = null;

        fishingData.unregister();
        fishingData = null;

        inventoryData.unregister();
        inventoryData = null;

        inventoryHeuristicsData.unregister();
        inventoryHeuristicsData = null;

        loginData.unregister();
        loginData = null;

        lookPacketData.unregister();
        lookPacketData = null;

        pingData.unregister();
        pingData = null;

        positionData.unregister();
        positionData = null;

        scaffoldData.unregister();
        scaffoldData = null;

        skinData = null;

        teamingData.unregister();
        teamingData = null;

        teleportData.unregister();
        teleportData = null;

        towerData.unregister();
        towerData = null;

        velocityChangeData.unregister();
        velocityChangeData = null;
    }
}
