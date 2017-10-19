package de.photon.AACAdditionPro.userdata;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.Permissions;
import de.photon.AACAdditionPro.userdata.data.*;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.UUID;

@Getter
public class User
{
    private final Player player;

    private final AutoPotionData autoPotionData = new AutoPotionData(this);
    private final BlockPlaceData scaffoldData = new BlockPlaceData(true, AACAdditionPro.getInstance().getConfig().getInt(AdditionHackType.SCAFFOLD.getConfigString() + ".buffer_size"), this)
    {
        @Override
        public double calculateRealTime()
        {
            // fraction[0] is the enumerator
            // fraction[1] is the divider
            final double[] fraction = new double[2];

            this.blockPlaces.clearLastObjectIteration(
                    (last, current) ->
                    {
                        final double speed_modifier;
                        if (current.getSpeedLevel() == null ||
                            current.getSpeedLevel() < 0)
                        {
                            speed_modifier = 1.0D;
                        }
                        else
                        {
                            //If the speedLevel is <= 0, the speed_modifier is 1
                            switch (current.getSpeedLevel())
                            {
                                case 0:
                                    speed_modifier = 1.01D;
                                    break;
                                case 1:
                                case 2:
                                case 3:
                                case 4:
                                case 5:
                                    speed_modifier = 1.5D;
                                    break;
                                case 6:
                                    speed_modifier = 1.55D;
                                    break;
                                case 7:
                                    speed_modifier = 2.3D;
                                    break;
                                default:
                                    // Everything above 8 should have a speed_modifier of 3
                                    speed_modifier = 3.0D;
                                    break;
                            }
                        }

                        // last - current to calculate the delta as the more recent time is always in last.
                        fraction[0] += (last.getTime() - current.getTime()) * speed_modifier;
                        fraction[1]++;
                    });
            return fraction[0] / fraction[1];
        }
    };
    private final BlockPlaceData towerData = new BlockPlaceData(false, AACAdditionPro.getInstance().getConfig().getInt(AdditionHackType.TOWER.getConfigString() + ".buffer_size"), this);
    private final ClientSideEntityData clientSideEntityData = new ClientSideEntityData(this);
    private final ElytraData elytraData = new ElytraData(this);
    private final EspInformationData espInformationData = new EspInformationData(this);
    private final TimeData fastSwitchData = new TimeData(false, this);
    private final FishingData fishingData = new FishingData(this);
    private final FlyPatchData flyPatchData = new FlyPatchData();
    private final InventoryData inventoryData = new InventoryData(this);
    private final LookPacketData lookPacketData = new LookPacketData(this);
    private final TimeData loginData;
    private final PingData pingData = new PingData(this);
    private final PositionData positionData = new PositionData(this);
    private final PotionData potionData = new PotionData(this);
    private final SettingsData settingsData = new SettingsData();
    private final TeamingData teamingData = new TeamingData(this);
    private final TeleportData teleportData = new TeleportData(this);
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
     * @return true if the uuid refers to this {@link User}
     */
    public boolean refersToUUID(final UUID uuid)
    {
        return uuid.equals(this.player.getUniqueId());
    }
}