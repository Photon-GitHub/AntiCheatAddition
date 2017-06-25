package de.photon.AACAdditionPro.checks.subchecks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerPosition;
import de.photon.AACAdditionPro.util.storage.management.ViolationLevelManagement;
import me.konsolas.aac.api.AACAPIProvider;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import java.util.HashSet;

public class Pingspoof extends PacketAdapter implements Listener, AACAdditionProCheck
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getAdditionHackType(), 500L);

    @LoadFromConfiguration(configPath = ".ping_offset")
    private double ping_offset;
    @LoadFromConfiguration(configPath = ".max_real_ping")
    private double max_real_ping;

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
        if (user == null || user.isBypassed()) {
            return;
        }

        // Checking right now
        if (user.getPingData().isCurrentlyChecking && user.getPingData().teleportLocation != null &&
            // Little distance from the real location
            Math.abs(event.getPacket().getDoubles().readSafely(0) - user.getPingData().teleportLocation.getX()) < 0.5 &&
            event.getPacket().getDoubles().readSafely(1) == user.getPingData().teleportLocation.getY() &&
            Math.abs(event.getPacket().getDoubles().readSafely(2) - user.getPingData().teleportLocation.getZ()) < 0.5 &&
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
        final int ping = (int) (System.currentTimeMillis() - user.getPingData().getTimeStamp());
        final int nmsPing = AACAPIProvider.getAPI().getPing(user.getPlayer());

        if (ping > this.max_real_ping) {
            return;
        }

        if (    // Ping over 0
                nmsPing > 0 &&
                // Offset between measured ping and nmsping too big
                nmsPing > ping * this.ping_offset)
        {
            vlManager.flag(user.getPlayer(), -1, () -> {}, () -> {});
            //VerboseSender.sendVerboseMessage("NMS: " + nmsPing + " | Measured: " + ping);
        }
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public AdditionHackType getAdditionHackType()
    {
        return AdditionHackType.PINGSPOOF;
    }

    @Override
    public void subEnable()
    {
        // Task
        task_number = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                AACAdditionPro.getInstance(),
                () ->
                {
                    for (final User user : UserManager.getUsers()) {
                        //Took too long to check
                        if (user.getPingData().isCurrentlyChecking && user.getPingData().recentlyUpdated(1000)) {
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
                                user.getPositionData().hasPlayerMovedRecently(Freecam.getIdle_time(), false) &&
                                // The Player has a high Ping
                                AACAPIProvider.getAPI().getPing(user.getPlayer()) > max_real_ping * ping_offset &&
                                // Safe-Time upon login as of fluctuating ping
                                !user.getLoginData().recentlyUpdated(10000))
                        {
                            user.getPingData().isCurrentlyChecking = true;

                            user.getPingData().teleportLocation = user.getPlayer().getLocation();
                            user.getPingData().updateTimeStamp();

                            final WrapperPlayServerPosition wrapperPlayServerPosition = new WrapperPlayServerPosition();
                            wrapperPlayServerPosition.setFlags(
                                    new HashSet<WrapperPlayServerPosition.PlayerTeleportFlag>()
                                    {{
                                        add(WrapperPlayServerPosition.PlayerTeleportFlag.X);
                                        add(WrapperPlayServerPosition.PlayerTeleportFlag.Y);
                                        add(WrapperPlayServerPosition.PlayerTeleportFlag.Z);
                                        add(WrapperPlayServerPosition.PlayerTeleportFlag.Y_ROT);
                                        add(WrapperPlayServerPosition.PlayerTeleportFlag.X_ROT);
                                    }});

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
}