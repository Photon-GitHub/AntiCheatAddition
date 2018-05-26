package de.photon.AACAdditionPro.checks.subchecks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.user.data.KeepAliveData;
import de.photon.AACAdditionPro.user.data.PositionData;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import de.photon.AACAdditionPro.util.packetwrappers.IWrapperPlayClientLook;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayClientKeepAlive;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayClientLook;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayClientPositionLook;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerKeepAlive;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class PacketAnalysis extends PacketAdapter implements ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 100);

    private BukkitTask injectTask;

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

    public PacketAnalysis()
    {
        super(AACAdditionPro.getInstance(), ListenerPriority.LOW,
              // KeepAlive analysis
              PacketType.Play.Server.KEEP_ALIVE,
              PacketType.Play.Client.KEEP_ALIVE,
              // EqualRotation
              PacketType.Play.Client.POSITION_LOOK,
              PacketType.Play.Client.LOOK);
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
            user.getKeepAliveData().getKeepAlives().add(new KeepAliveData.KeepAlivePacketData(serverKeepAliveWrapper.getKeepAliveId()));

            System.out.println("Out: " + serverKeepAliveWrapper.getKeepAliveId());

            // Check on sending to force the client to respond in a certain time-frame.
            if (keepAlive &&
                keepAliveIgnored &&
                user.getKeepAliveData().getKeepAlives().size() > KeepAliveData.KEEPALIVE_QUEUE_SIZE &&
                !user.getKeepAliveData().getKeepAlives().remove(0).hasRegisteredResponse())
            {
                VerboseSender.sendVerboseMessage("PacketAnalysis-Verbose | Player: " + user.getPlayer().getName() + " ignored KeepAlive packet.");
                vlManager.flag(user.getPlayer(), 10, () -> {}, () -> {});
            }
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

        // EqualRotation part
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
                VerboseSender.sendVerboseMessage("PacketAnalysis: received invalid packet: " + event.getPacketType().toString(), true, true);
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
                VerboseSender.sendVerboseMessage("PacketAnalysis-Verbose | Player: " + user.getPlayer().getName() + " sent equal rotations.");
                vlManager.flag(user.getPlayer(), -1, () -> {}, () -> {});
            }
            else
            {
                user.getLookPacketData().updateRotations(currentYaw, currentPitch);
            }
        }

        // KeepAlive part
        if (keepAlive &&
            // Correct packet
            event.getPacketType() == PacketType.Play.Client.KEEP_ALIVE)
        {
            final WrapperPlayClientKeepAlive clientKeepAliveWrapper = new WrapperPlayClientKeepAlive(event.getPacket());
            KeepAliveData.KeepAlivePacketData keepAlivePacketData = null;

            System.out.println("In: " + clientKeepAliveWrapper.getKeepAliveId());

            int index = user.getKeepAliveData().getKeepAlives().size() - 1;
            while (index >= 0)
            {
                KeepAliveData.KeepAlivePacketData alivePacketData = user.getKeepAliveData().getKeepAlives().get(index);
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
                VerboseSender.sendVerboseMessage("PacketAnalysis-Verbose | Player: " + user.getPlayer().getName() + " sent unregistered KeepAlive packet.");
                vlManager.flag(user.getPlayer(), 20, -1, () -> {}, () -> {});
            }
            else
            {
                keepAlivePacketData.registerResponse();

                if (keepAliveOffset &&
                    user.getKeepAliveData().getKeepAlives().size() == KeepAliveData.KEEPALIVE_QUEUE_SIZE)
                {
                    // -1 because of size -> index conversion
                    final int offset = (KeepAliveData.KEEPALIVE_QUEUE_SIZE - 1) - index;
                    if (offset > 0)
                    {
                        VerboseSender.sendVerboseMessage("PacketAnalysis-Verbose | Player: " + user.getPlayer().getName() + " sent packets out of order with an offset of: " + offset);
                        vlManager.flag(user.getPlayer(), Math.min(KeepAliveData.KEEPALIVE_QUEUE_SIZE - index, 10), -1, () -> {}, () -> {});
                    }
                }
            }
        }
    }

    private void recursiveKeepAliveInjection()
    {
        injectTask = Bukkit.getScheduler().runTaskLaterAsynchronously(
                AACAdditionPro.getInstance(),
                () ->
                {
                    System.out.println("Time: " + System.currentTimeMillis());
                    int time = (int) System.currentTimeMillis();
                    for (final User user : UserManager.getUsersUnwrapped())
                    {
                        // Not bypassed
                        if (!user.isBypassed())
                        {
                            final WrapperPlayServerKeepAlive wrapperPlayServerKeepAlive = new WrapperPlayServerKeepAlive();
                            System.out.println("Inject: " + time);
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
        keepAlive = keepAliveUnregistered || keepAliveIgnored || keepAliveOffset || keepAliveInject;

        // Unregistered must be enabled to use offset analysis.
        if (keepAlive && !keepAliveUnregistered)
        {
            keepAlive = false;
            VerboseSender.sendVerboseMessage("PacketAnalysis | Failed to enable KeepAlive part", true, true);
            VerboseSender.sendVerboseMessage("PacketAnalysis | In order to use the KeepAlive you need to enable the unregistered anaysis!", true, true);
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
