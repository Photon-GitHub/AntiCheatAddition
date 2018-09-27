package de.photon.AACAdditionPro.modules.checks.packetanalysis;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.packetwrappers.client.WrapperPlayClientPositionLook;
import de.photon.AACAdditionPro.util.world.LocationUtils;

/**
 * This {@link de.photon.AACAdditionPro.modules.PatternModule.PacketPattern} detects spoofed positions.
 */
class PositionSpoofPattern extends PatternModule.PacketPattern
{
    PositionSpoofPattern()
    {
        // Response to PacketType.Play.Client.POSITION
        super(ImmutableSet.of(PacketType.Play.Client.POSITION_LOOK));
    }

    @Override
    protected int process(User user, PacketContainer packetContainer)
    {
        final WrapperPlayClientPositionLook clientPositionLookWrapper = new WrapperPlayClientPositionLook(packetContainer);

        // Only check if the player has been teleported recently
        if (user.getTeleportData().recentlyUpdated(0, 1000) &&
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

            if (!LocationUtils.areLocationsInRange(user.getPacketAnalysisData().lastPositionForceData.getLocation(), clientPositionLookWrapper.getLocation(user.getPlayer().getWorld()), allowedDistance))
            {
                VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " tried to spoof position packets.");
                return 10;
            }
        }
        return 0;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.PositionSpoof";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.PACKET_ANALYSIS;
    }
}
