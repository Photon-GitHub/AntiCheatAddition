package de.photon.aacadditionpro.modules.checks.packetanalysis;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.modules.IncompatiblePluginModule;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.user.DataKey;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.messaging.VerboseSender;
import de.photon.aacadditionpro.util.packetwrappers.client.WrapperPlayClientPositionLook;
import de.photon.aacadditionpro.util.world.LocationUtils;
import lombok.Getter;
import org.bukkit.Location;

import java.util.Set;
import java.util.function.BiConsumer;

/**
 * This {@link Module} detects spoofed positions.
 */
class PositionSpoofPattern implements IncompatiblePluginModule
{
    @Getter
    private static final PositionSpoofPattern instance = new PositionSpoofPattern();

    @Getter
    private BiConsumer<User, PacketEvent> applyingConsumer = (user, packetEvent) -> {};

    @Override
    public void enable()
    {
        applyingConsumer = (user, packetEvent) -> {
            if (packetEvent.getPacketType() != PacketType.Play.Client.POSITION_LOOK) {
                return;
            }

            final WrapperPlayClientPositionLook clientPositionLookWrapper = new WrapperPlayClientPositionLook(packetEvent.getPacket());

            // Only check if the player has been teleported recently
            if (user.hasTeleportedRecently(1000) &&
                // World changes and respawns are exempted
                !user.hasRespawnedRecently(2500) &&
                !user.hasChangedWorldsRecently(2500) &&
                // Lag occurrences after login.
                !user.hasLoggedInRecently(10000))
            {
                // The position packet might not be exactly the same position.
                // Squared values of 10, 5 and 3
                final double allowedDistanceSquared = user.getPlayer().isFlying() ?
                                                      100 :
                                                      (user.getPlayer().isSprinting() ? 25 : 9);

                final Location forcedLocation = (Location) user.getDataMap().getValue(DataKey.PACKET_ANALYSIS_LAST_POSITION_FORCE_LOCATION);

                if (LocationUtils.inSameWorld(forcedLocation, user.getPlayer().getLocation())) {
                    final double distanceSquared = forcedLocation.distanceSquared(clientPositionLookWrapper.getLocation(user.getPlayer().getWorld()));

                    if (distanceSquared > allowedDistanceSquared) {
                        PacketAnalysis.getInstance().getViolationLevelManagement().flag(user.getPlayer(), 10,
                                                                                        -1, () -> {},
                                                                                        () -> VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " tried to spoof position packets. | DS: " + distanceSquared + " ADS: " + allowedDistanceSquared));
                    }
                } else {
                    VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " diff world.");
                }
            }
        };
    }

    @Override
    public void disable()
    {
        applyingConsumer = (user, packet) -> {};
    }

    @Override
    public Set<String> getIncompatiblePlugins()
    {
        return ImmutableSet.of("ProtocolSupport");
    }

    @Override
    public boolean isSubModule()
    {
        return true;
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
