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
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import de.photon.AACAdditionPro.util.packetwrappers.IWrapperPlayClientLook;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;

public class SmoothAim extends PacketAdapter implements ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 20);

    @LoadFromConfiguration(configPath = ".rotation_threshold")
    private int rotation_threshold;

    @LoadFromConfiguration(configPath = ".angle_range")
    private int angle_range;

    public SmoothAim()
    {
        super(AACAdditionPro.getInstance(), ListenerPriority.LOW,
              PacketType.Play.Client.POSITION_LOOK,
              // EqualRotation
              PacketType.Play.Client.LOOK);
    }

    @Override
    public void onPacketReceiving(PacketEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user) ||
            // Not in vehicle (possible false positive)
            user.getPlayer().isInsideVehicle() ||
            // Not recently teleported
            user.getTeleportData().recentlyUpdated(0, 5000) ||
            // User must be attacking something
            !user.getSmoothAimData().recentlyUpdated(0, 2500) ||
            // The player has to be moving.
            !user.getPositionData().hasPlayerMovedRecently(100, PositionData.MovementType.XZONLY))
        {
            return;
        }

        final IWrapperPlayClientLook lookWrapper = event::getPacket;

        // Both yaw and pitch are in range.
        if (lookWrapper.getYaw() != user.getLookPacketData().getRealLastYaw() &&
            MathUtils.roughlyEquals(lookWrapper.getYaw(), user.getLookPacketData().getRealLastYaw(), angle_range) &&
            lookWrapper.getPitch() != user.getLookPacketData().getRealLastPitch() &&
            MathUtils.roughlyEquals(lookWrapper.getPitch(), user.getLookPacketData().getRealLastPitch(), angle_range))
        {
            // Prevent false positives
            if (++user.getSmoothAimData().smoothAimCounter > rotation_threshold)
                vlManager.flag(user.getPlayer(), -1, () -> {}, () -> {});
        }
        else
        {
            // Reset the counter if a legit action is recorded.
            user.getSmoothAimData().smoothAimCounter = 0;
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
        return ModuleType.SMOOTHAIM;
    }
}
