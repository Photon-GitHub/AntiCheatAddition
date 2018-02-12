package de.photon.AACAdditionPro.checks.subchecks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.user.data.PositionData;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerPosition;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;
import me.konsolas.aac.api.AACAPIProvider;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

public class Pingspoof extends PacketAdapter implements Listener, ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 500L);

    @LoadFromConfiguration(configPath = ".ping_offset")
    private double ping_offset;
    @LoadFromConfiguration(configPath = ".max_real_ping")
    private double max_real_ping;
    @LoadFromConfiguration(configPath = ".refresh_at_vl")
    private int refresh_at_vl;

    private int task_number;

    public Pingspoof()
    {
        super(AACAdditionPro.getInstance(), PacketType.Play.Client.POSITION_LOOK);
    }

    @Override
    public void onPacketReceiving(final PacketEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user))
        {
            return;
        }

        // Checking right now
        if (user.getPingData().isCurrentlyChecking && user.getPingData().teleportLocation != null &&
            // Little distance from the real location
            MathUtils.roughlyEquals(event.getPacket().getDoubles().readSafely(0), user.getPingData().teleportLocation.getX(), 0.5D) &&
            // Also little difference here as of potential y - changes (i.e. on water lilies, redstone, etc.)
            MathUtils.roughlyEquals(event.getPacket().getDoubles().readSafely(1), user.getPingData().teleportLocation.getY(), 0.14D) &&
            MathUtils.roughlyEquals(event.getPacket().getDoubles().readSafely(2), user.getPingData().teleportLocation.getZ(), 0.5D) &&
            !event.getPacket().getBooleans().readSafely(0))
        {
            this.check(user);

            user.getPingData().teleportLocation = null;
            user.getPingData().isCurrentlyChecking = false;

            event.setCancelled(true);
        }
    }

    private void check(final User user)
    {
        final long ping = user.getPingData().passedTime(0);
        final long nmsPing = AACAPIProvider.getAPI().getPing(user.getPlayer());

        /*
            If the measured ping is too high for a sophisticated result or a ping-update is scheduled soon ignore this to
            prevent false positives
        */
        if (ping > this.max_real_ping || user.getPingData().forceUpdatePing)
        {
            return;
        }

        if (    // Ping over 0
                nmsPing > 0 &&
                // Offset between measured ping and nmsping too big
                nmsPing > ping * this.ping_offset)
        {
            // Use the cancel-system as a refresh system here (cancel has no use regarding pings).
            vlManager.flag(user.getPlayer(), refresh_at_vl, () -> user.getPingData().forceUpdatePing = true, () -> {});
            //VerboseSender.sendVerboseMessage("NMS: " + nmsPing + " | Measured: " + ping);
        }
    }

    @Override
    public void subEnable()
    {
        // Task
        task_number = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                AACAdditionPro.getInstance(),
                () ->
                {
                    for (final User user : UserManager.getUsersUnwrapped())
                    {
                        //Took too long to check
                        if (user.getPingData().isCurrentlyChecking && user.getPingData().recentlyUpdated(0, 1000))
                        {
                            user.getPingData().teleportLocation = null;
                            user.getPingData().isCurrentlyChecking = false;
                        }

                        if (    //Not bypassed
                                !user.isBypassed() &&
                                // Not currently checking
                                !user.getPingData().isCurrentlyChecking &&
                                // Player is not in an Inventory
                                !user.getInventoryData().hasOpenInventory() &&
                                // Player is onGround
                                user.getPlayer().isOnGround() &&
                                // Player moving (Freecam compatibility)
                                user.getPositionData().hasPlayerMovedRecently(Freecam.getIdle_time(), PositionData.MovementType.NONHEAD) &&
                                // The Player has a high ping or the check is scheduled
                                (user.getPingData().forceUpdatePing || AACAPIProvider.getAPI().getPing(user.getPlayer()) > max_real_ping * ping_offset) &&
                                // Safe-Time upon login as of fluctuating ping
                                !user.getLoginData().recentlyUpdated(0, 10000))
                        {
                            // Now checking
                            user.getPingData().isCurrentlyChecking = true;
                            // Update should only be run once.
                            user.getPingData().forceUpdatePing = false;

                            user.getPingData().teleportLocation = user.getPlayer().getLocation();
                            user.getPingData().updateTimeStamp(0);

                            final WrapperPlayServerPosition wrapperPlayServerPosition = new WrapperPlayServerPosition();
                            wrapperPlayServerPosition.setAllFlags();

                            wrapperPlayServerPosition.sendPacket(user.getPlayer());
                        }
                    }
                }, 1000L, 100L);
    }

    @Override
    public void subDisable()
    {
        Bukkit.getScheduler().cancelTask(task_number);
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.PINGSPOOF;
    }
}