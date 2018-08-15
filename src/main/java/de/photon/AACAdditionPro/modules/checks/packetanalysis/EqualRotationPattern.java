package de.photon.AACAdditionPro.modules.checks.packetanalysis;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.data.PositionData;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.packetwrappers.IWrapperPlayClientLook;

class EqualRotationPattern extends PatternModule.PacketPattern
{
    EqualRotationPattern()
    {
        super(ImmutableSet.of(PacketType.Play.Client.POSITION_LOOK, PacketType.Play.Client.LOOK));
    }

    @Override
    protected int process(User user, PacketContainer packetContainer)
    {
        // Get the packet.
        final IWrapperPlayClientLook lookWrapper = () -> packetContainer;

        final float currentYaw = lookWrapper.getYaw();
        final float currentPitch = lookWrapper.getPitch();

        // Boat false positive (usually worse cheats in vehicles as well)
        if (!user.getPlayer().isInsideVehicle() &&
            // Not recently teleported
            !user.getTeleportData().recentlyUpdated(0, 5000) &&
            // Same rotation values
            // LookPacketData automatically updates its values.
            currentYaw == user.getLookPacketData().getRealLastYaw() &&
            currentPitch == user.getLookPacketData().getRealLastPitch() &&
            // Labymod fp when standing still / hit in corner fp
            user.getPositionData().hasPlayerMovedRecently(100, PositionData.MovementType.XZONLY))
        {
            VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " sent equal rotations.");
            return 1;
        }
        return 0;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.EqualRotation";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.PACKET_ANALYSIS;
    }
}
