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
import de.photon.AACAdditionPro.user.data.PositionData;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.packetwrappers.IWrapperPlayClientLook;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayClientLook;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayClientPositionLook;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;

public class EqualRotation extends PacketAdapter implements ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 100);

    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancel_vl;
    @LoadFromConfiguration(configPath = ".timeout")
    private int timeout;

    public EqualRotation()
    {
        super(AACAdditionPro.getInstance(), ListenerPriority.LOW, PacketType.Play.Client.POSITION_LOOK, PacketType.Play.Client.LOOK);
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
            VerboseSender.sendVerboseMessage("EqualRotation: received invalid packet: " + event.getPacketType().toString(), true, true);
            return;
        }

        if (user.getLookPacketData().recentlyUpdated(0, this.timeout))
        {
            lookWrapper.setYaw(user.getLookPacketData().getRealLastYaw());
            lookWrapper.setYaw(user.getLookPacketData().getRealLastPitch());
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
            vlManager.flag(user.getPlayer(), cancel_vl, () ->
            {
                lookWrapper.setYaw(user.getLookPacketData().getRealLastYaw());
                lookWrapper.setYaw(user.getLookPacketData().getRealLastPitch());
                user.getLookPacketData().updateTimeStamp(0);
            }, () -> {});
        }
        else
        {
            user.getLookPacketData().updateRotations(currentYaw, currentPitch);
        }
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.EQUAL_ROTATION;
    }
}
