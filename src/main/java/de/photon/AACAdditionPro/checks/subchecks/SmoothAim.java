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
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 600);

    @LoadFromConfiguration(configPath = ".rotation_threshold")
    private int rotation_threshold;

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
            // The player has to be moving.
            !user.getPositionData().hasPlayerMovedRecently(100, PositionData.MovementType.XZONLY))
        {
            return;
        }

        final IWrapperPlayClientLook lookWrapper = event::getPacket;

        // Both yaw and pitch are in range.
        if (lookWrapper.getYaw() != user.getLookPacketData().getRealLastYaw() &&
            MathUtils.roughlyEquals(lookWrapper.getYaw(), user.getLookPacketData().getRealLastYaw(), 5) &&
            lookWrapper.getPitch() != user.getLookPacketData().getRealLastPitch() &&
            MathUtils.roughlyEquals(lookWrapper.getPitch(), user.getLookPacketData().getRealLastPitch(), 5) &&
            ++user.getLookPacketData().smoothAimCounter > rotation_threshold)
        {
            vlManager.flag(user.getPlayer(), -1, () -> {}, () -> {});
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
