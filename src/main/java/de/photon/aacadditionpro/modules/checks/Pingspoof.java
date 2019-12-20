package de.photon.aacadditionpro.modules.checks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ListenerModule;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PacketListenerModule;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.mathematics.MathUtils;
import de.photon.aacadditionpro.util.packetwrappers.server.WrapperPlayServerPosition;
import de.photon.aacadditionpro.util.server.ServerUtil;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import org.bukkit.Bukkit;

public class Pingspoof extends PacketAdapter implements ListenerModule, PacketListenerModule, ViolationModule
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
        if (User.isUserInvalid(user, this.getModuleType())) {
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
        final long nmsPing = ServerUtil.getPing(user.getPlayer());

        /*
            If the measured ping is too high for a sophisticated result or a ping-update is scheduled soon ignore this to
            prevent false positives
        */
        if (ping > this.max_real_ping || user.getPingData().forceUpdatePing) {
            return;
        }

        if (    // Ping over 0
                nmsPing > 0 &&
                // Offset between measured ping and nmsping too big
                nmsPing > ping * this.ping_offset)
        {
            // Use the cancel-system as a refresh system here (cancel has no use regarding pings).
            vlManager.flag(user.getPlayer(), true, refresh_at_vl, () -> user.getPingData().forceUpdatePing = true, () -> {});
            //VerboseSender.sendVerboseMessage("NMS: " + nmsPing + " | Measured: " + ping);
        }
    }

    @Override
    public void enable()
    {
        // Task
        task_number = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                AACAdditionPro.getInstance(),
                () ->
                {
                    for (final User user : UserManager.getUsersUnwrapped()) {
                        //Took too long to check
                        if (user.getPingData().isCurrentlyChecking && user.getPingData().recentlyUpdated(0, 1000)) {
                            user.getPingData().teleportLocation = null;
                            user.getPingData().isCurrentlyChecking = false;
                        }

                        if (    //Not bypassed
                                !user.isBypassed(this.getModuleType()) &&
                                // Not currently checking
                                !user.getPingData().isCurrentlyChecking &&
                                // Player is not in an Inventory
                                !user.getInventoryData().hasOpenInventory() &&
                                // Player is onGround
                                user.getPlayer().isOnGround() &&
                                // The Player has a high ping or the check is scheduled
                                (user.getPingData().forceUpdatePing || ServerUtil.getPing(user.getPlayer()) > max_real_ping * ping_offset) &&
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
    public void disable()
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