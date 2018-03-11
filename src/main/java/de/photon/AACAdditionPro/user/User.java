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
    private final Player player;

    private final AutoPotionData autoPotionData = new AutoPotionData(this);
    private final ClientSideEntityData clientSideEntityData = new ClientSideEntityData(this);
    private final EspInformationData espInformationData = new EspInformationData(this);
    private final TimeData fastSwitchData = new TimeData(this);
    private final FishingData fishingData = new FishingData(this);
    private final InventoryData inventoryData = new InventoryData(this);
    private final InventoryHeuristicsData inventoryHeuristicsData = new InventoryHeuristicsData();
    private final TimeData loginData;
    private final LookPacketData lookPacketData = new LookPacketData(this);
    private final PingData pingData = new PingData(this);
    private final PositionData positionData = new PositionData(this);
    private final ScaffoldData scaffoldData = new ScaffoldData(this);
    private final SkinData skinData = new SkinData();
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
        autoPotionData.unregister();
        clientSideEntityData.unregister();
        espInformationData.unregister();
        fastSwitchData.unregister();
        fishingData.unregister();
        inventoryData.unregister();
        clientSideEntityData.unregister();
        espInformationData.unregister();
        fastSwitchData.unregister();
        fishingData.unregister();
        inventoryData.unregister();
        loginData.unregister();
        lookPacketData.unregister();
        pingData.unregister();
        positionData.unregister();
        scaffoldData.unregister();
        teamingData.unregister();
        teleportData.unregister();
        towerData.unregister();
        velocityChangeData.unregister();
    }
}
