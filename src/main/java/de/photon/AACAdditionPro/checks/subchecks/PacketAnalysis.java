package de.photon.AACAdditionPro.checks.subchecks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.ServerVersion;
import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.user.data.PacketAnalysisData;
import de.photon.AACAdditionPro.user.data.PositionData;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import de.photon.AACAdditionPro.util.packetwrappers.IWrapperPlayClientLook;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayClientKeepAlive;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayClientLook;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayClientPositionLook;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerKeepAlive;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerPosition;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class PacketAnalysis extends PacketAdapter implements ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 200);

    private BukkitTask injectTask;

    @LoadFromConfiguration(configPath = ".parts.Compare.enabled")
    private boolean compare;
    @LoadFromConfiguration(configPath = ".parts.Compare.allowed_offset")
    private int allowedOffset;
    @LoadFromConfiguration(configPath = ".parts.Compare.compare_threshold")
    private int compareThreshold;
    @LoadFromConfiguration(configPath = ".parts.Compare.violation_time")
    private int violationTime;

    @LoadFromConfiguration(configPath = ".parts.EqualRotatíon.enabled")
    private boolean equalRotation;

    private boolean keepAlive;
    @LoadFromConfiguration(configPath = ".parts.KeepAlive.unregistered.enabled")
    private boolean keepAliveUnregistered;
    @LoadFromConfiguration(configPath = ".parts.KeepAlive.ignored.enabled")
    private boolean keepAliveIgnored;
    @LoadFromConfiguration(configPath = ".parts.KeepAlive.offset.enabled")
    private boolean keepAliveOffset;
    @LoadFromConfiguration(configPath = ".parts.KeepAlive.inject.enabled")
    private boolean keepAliveInject;

    @LoadFromConfiguration(configPath = ".parts.PositionSpoof.enabled")
    private boolean positionSpoof;

    @LoadFromConfiguration(configPath = ".parts.TimeManipulation.enabled")
    private boolean timeManipulation;
    @LoadFromConfiguration(configPath = ".parts.TimeManipulation.detection_millis")
    private int detectionMillis;

    public PacketAnalysis()
    {
        super(AACAdditionPro.getInstance(), ListenerPriority.LOW,
              // Compare
              PacketType.Play.Server.POSITION,
              // KeepAlive analysis
              PacketType.Play.Server.KEEP_ALIVE,
              PacketType.Play.Client.KEEP_ALIVE,
              // EqualRotation + Compare
              PacketType.Play.Client.POSITION_LOOK,
              // EqualRotation
              PacketType.Play.Client.LOOK,
              // Time manipulation
              PacketType.Play.Client.FLYING);
    }

    @Override
    public void onPacketSending(PacketEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user) || !keepAlive)
        {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.KEEP_ALIVE)
        {
            final WrapperPlayServerKeepAlive serverKeepAliveWrapper = new WrapperPlayServerKeepAlive(event.getPacket());
            user.getPacketAnalysisData().getKeepAlives().add(new PacketAnalysisData.KeepAlivePacketData(serverKeepAliveWrapper.getKeepAliveId()));

            // Check on sending to force the client to respond in a certain time-frame.
            if (keepAlive &&
                keepAliveIgnored &&
                user.getPacketAnalysisData().getKeepAlives().size() > PacketAnalysisData.KEEPALIVE_QUEUE_SIZE &&
                !user.getPacketAnalysisData().getKeepAlives().remove(0).hasRegisteredResponse())
            {
                VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " ignored KeepAlive packet.");
                vlManager.flag(user.getPlayer(), 10, -1, () -> {}, () -> {});
            }
        }
        else if (event.getPacketType() == PacketType.Play.Server.POSITION)
        {
            final WrapperPlayServerPosition serverPositionWrapper = new WrapperPlayServerPosition(event.getPacket());
            user.getPacketAnalysisData().lastPositionForceData = new PacketAnalysisData.PositionForceData(serverPositionWrapper.getLocation(user.getPlayer().getWorld()));
        }
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

        // --------------------------------------------- EqualRotation ---------------------------------------------- //
        if (equalRotation &&
            // Correct packets
            (event.getPacketType() == PacketType.Play.Client.POSITION_LOOK || event.getPacketType() == PacketType.Play.Client.LOOK))
        {
            final IWrapperPlayClientLook lookWrapper;

            // Differentiate the packets
            if (event.getPacketType() == PacketType.Play.Client.POSITION_LOOK)
            {
                // PositionLook wrapper
                lookWrapper = new WrapperPlayClientPositionLook(event.getPacket());
            }
            else if (event.getPacketType() == PacketType.Play.Client.LOOK)
            {
                // Look wrapper
                lookWrapper = new WrapperPlayClientLook(event.getPacket());
            }
            else
            {
                VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData: received invalid packet: " + event.getPacketType().toString(), true, true);
                return;
            }

            final float currentYaw = lookWrapper.getYaw();
            final float currentPitch = lookWrapper.getPitch();

            // Boat false positive (usually worse cheats in vehicles as well)
            if (!user.getPlayer().isInsideVehicle() &&
                // Not recently teleported
                !user.getTeleportData().recentlyUpdated(0, 5000) &&
                // Same rotation values
                currentYaw == user.getLookPacketData().getRealLastYaw() &&
                currentPitch == user.getLookPacketData().getRealLastPitch() &&
                // Labymod fp when standing still / hit in corner fp
                user.getPositionData().hasPlayerMovedRecently(100, PositionData.MovementType.XZONLY))
            {
                VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " sent equal rotations.");
                vlManager.flag(user.getPlayer(), -1, () -> {}, () -> {});
            }
            else
            {
                user.getLookPacketData().updateRotations(currentYaw, currentPitch);
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.KEEP_ALIVE)
        {
            // --------------------------------------------- KeepAlive ---------------------------------------------- //
            if (keepAlive)
            {
                final WrapperPlayClientKeepAlive clientKeepAliveWrapper = new WrapperPlayClientKeepAlive(event.getPacket());
                PacketAnalysisData.KeepAlivePacketData keepAlivePacketData = null;

                int index = user.getPacketAnalysisData().getKeepAlives().size() - 1;
                while (index >= 0)
                {
                    PacketAnalysisData.KeepAlivePacketData alivePacketData = user.getPacketAnalysisData().getKeepAlives().get(index);
                    if (alivePacketData.getKeepAliveID() == clientKeepAliveWrapper.getKeepAliveId())
                    {
                        keepAlivePacketData = alivePacketData;
                        break;
                    }
                    index--;
                }

                // A packet with the same data must have been sent before.
                if (keepAlivePacketData == null)
                {
                    VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " sent unregistered KeepAlive packet.");
                    vlManager.flag(user.getPlayer(), 20, -1, () -> {}, () -> {});
                }
                else
                {
                    keepAlivePacketData.registerResponse();

                    if (keepAliveOffset &&
                        user.getPacketAnalysisData().getKeepAlives().size() == PacketAnalysisData.KEEPALIVE_QUEUE_SIZE)
                    {
                        // -1 because of size -> index conversion
                        final int offset = (PacketAnalysisData.KEEPALIVE_QUEUE_SIZE - 1) - index;
                        if (offset > 0)
                        {
                            VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " sent packets out of order with an offset of: " + offset);
                            vlManager.flag(user.getPlayer(), Math.min((PacketAnalysisData.KEEPALIVE_QUEUE_SIZE - index) * 2, 10), -1, () -> {}, () -> {});
                        }
                    }
                }
            }

            // ------------------------------------------ TimeManipulation ------------------------------------------ //
            if (timeManipulation)
            {
                // FLYING is only sent if the player does not move (both body and head).
                if (!user.getPositionData().hasPlayerMovedRecently(detectionMillis, PositionData.MovementType.ANY) &&
                    !user.getPacketAnalysisData().recentlyUpdated(0, detectionMillis) &&
                    // False positives e.g. in the void or in the air.
                    !user.getPlayer().isDead() &&
                    // This check will cause false positives with client versions higher than 1.8.8
                    ServerVersion.getClientServerVersion(user.getPlayer()) == ServerVersion.MC188)
                {
                    VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " may be manipulating time on a protocol level.");
                    vlManager.flag(user.getPlayer(), 2, -1, () -> {}, () -> {});
                }
            }
        }

        // ----------------------------------------- Compare + PositionSpoof ---------------------------------------- //
        // Response to Play.Server.POSITION
        if (event.getPacketType() == PacketType.Play.Client.POSITION_LOOK)
        {
            final WrapperPlayClientPositionLook clientPositionLookWrapper = new WrapperPlayClientPositionLook(event.getPacket());

            if (user.getPacketAnalysisData().lastPositionForceData != null)
            {
                if (compare &&
                    // Make sure enough datapoints exist for checking.
                    user.getPacketAnalysisData().getKeepAlives().size() > 10)
                {
                    final double offset = MathUtils.offset(
                            user.getPacketAnalysisData().recentKeepAliveResponseTime(),
                            user.getPacketAnalysisData().lastPositionForceData.timeDifference()) - allowedOffset;

                    // Should flag
                    if (offset > 0)
                    {
                        // Minimum time between flags to decrease lag spike effects.
                        if (!user.getPacketAnalysisData().recentlyUpdated(1, violationTime) &&
                            // Minimum fails to mitigate some fluctuations
                            ++user.getPacketAnalysisData().compareFails > this.compareThreshold)
                        {
                            VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " sends packets with different delays.");
                            vlManager.flag(user.getPlayer(), Math.min(Math.max(1, (int) (offset / 50)), 12), -1, () -> {},
                                           // Only update the time stamp if flagged.
                                           () -> user.getPacketAnalysisData().updateTimeStamp(1));
                        }
                    }
                    else if (user.getPacketAnalysisData().compareFails > 0)
                    {
                        user.getPacketAnalysisData().compareFails--;
                    }
                }

                if (positionSpoof &&
                    // Only check if the player has been teleported recently
                    user.getTeleportData().recentlyUpdated(0, 1000) &&
                    // World changes and respawns are exempted
                    !user.getTeleportData().recentlyUpdated(1, 2500) &&
                    !user.getTeleportData().recentlyUpdated(2, 2500) &&
                    // Lag occurrences after login.
                    !user.getLoginData().recentlyUpdated(0, 10000))
                {
                    // The position packet might not be exactly the same position.
                    // Squared values of 10, 5 and 3
                    final double allowedDistance = user.getPlayer().isFlying() ?
                                                   100 :
                                                   (user.getPlayer().isSprinting() ? 25 : 9);

                    if (!MathUtils.areLocationsInRange(user.getPacketAnalysisData().lastPositionForceData.getLocation(), clientPositionLookWrapper.getLocation(user.getPlayer().getWorld()), allowedDistance))
                    {
                        VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " tried to spoof position packets.");
                        vlManager.flag(user.getPlayer(), 10, -1, () -> {}, () -> {});
                    }
                }

                // No continuous flagging.
                user.getPacketAnalysisData().lastPositionForceData = null;
            }
        }

        // -------------------------------------------- TimeManipulation -------------------------------------------- //
        // Only a 1.8.8 client sends FLYING when idling. 1.10+ clients will only send it on respawn or when using the
        // statistics.
        if (event.getPacketType() == PacketType.Play.Client.FLYING)
        {
            user.getPacketAnalysisData().updateTimeStamp(0);
        }
    }

    private void recursiveKeepAliveInjection()
    {
        injectTask = Bukkit.getScheduler().runTaskLaterAsynchronously(
                AACAdditionPro.getInstance(),
                () ->
                {
                    long time = System.nanoTime() / 1000000L;
                    for (final User user : UserManager.getUsersUnwrapped())
                    {
                        // Not bypassed
                        if (!user.isBypassed())
                        {
                            final WrapperPlayServerKeepAlive wrapperPlayServerKeepAlive = new WrapperPlayServerKeepAlive();
                            wrapperPlayServerKeepAlive.setKeepAliveId(time);
                            wrapperPlayServerKeepAlive.sendPacket(user.getPlayer());
                        }
                    }
                    recursiveKeepAliveInjection();
                }, MathUtils.randomBoundaryInt(80, 35));
    }

    @Override
    public void subEnable()
    {
        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
                break;
            case MC111:
            case MC112:
                keepAliveInject = false;
                timeManipulation = false;
                break;
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }

        keepAlive = keepAliveUnregistered || keepAliveIgnored || keepAliveOffset || keepAliveInject;

        // Unregistered must be enabled to use offset analysis.
        if (keepAlive && !keepAliveUnregistered)
        {
            keepAlive = false;
            VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData | Failed to enable KeepAlive part", true, true);
            VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData | In order to use the KeepAlive you need to enable the unregistered analysis!", true, true);
        }

        if (keepAlive && keepAliveInject)
        {
            recursiveKeepAliveInjection();
        }
    }


    @Override
    public void subDisable()
    {
        injectTask.cancel();
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.PACKET_ANALYSIS;
    }
}
