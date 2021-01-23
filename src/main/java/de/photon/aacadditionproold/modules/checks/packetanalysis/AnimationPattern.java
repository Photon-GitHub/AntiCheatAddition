package de.photon.aacadditionproold.modules.checks.packetanalysis;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import de.photon.aacadditionproold.AACAdditionPro;
import de.photon.aacadditionproold.ServerVersion;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.modules.PacketListenerModule;
import de.photon.aacadditionproold.modules.RestrictedServerVersion;
import de.photon.aacadditionproold.user.DataKey;
import de.photon.aacadditionproold.user.User;
import de.photon.aacadditionproold.user.UserManager;
import de.photon.aacadditionproold.util.messaging.VerboseSender;
import de.photon.aacadditionproold.util.packetwrappers.client.WrapperPlayClientUseEntity;
import lombok.Getter;

import java.util.Set;

public class AnimationPattern extends PacketAdapter implements PacketListenerModule, RestrictedServerVersion
{
    @Getter
    private static final AnimationPattern instance = new AnimationPattern();

    public AnimationPattern()
    {
        super(AACAdditionPro.getInstance(), ListenerPriority.LOW,
              // THIS IS IN THE ORDER OF HOW THE PACKETS ARE SUPPOSED TO ARRIVE.
              PacketType.Play.Client.USE_ENTITY,
              PacketType.Play.Client.ARM_ANIMATION);
    }


    @Override
    public void onPacketReceiving(final PacketEvent packetEvent)
    {
        final User user = UserManager.safeGetUserFromPacketEvent(packetEvent);

        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        if (packetEvent.getPacketType() == PacketType.Play.Client.ARM_ANIMATION) {
            user.getDataMap().setValue(DataKey.PACKET_ANALYSIS_ANIMATION_EXPECTED, false);
        } else {
            if (user.getDataMap().getBoolean(DataKey.PACKET_ANALYSIS_ANIMATION_EXPECTED)) {
                user.getDataMap().setValue(DataKey.PACKET_ANALYSIS_ANIMATION_EXPECTED, false);
                PacketAnalysis.getInstance().getViolationLevelManagement().flag(user.getPlayer(), 10,
                                                                                -1, () -> {},
                                                                                () -> VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " did not send animation packet after an attack."));
            }
        }

        if (packetEvent.getPacketType() == PacketType.Play.Client.USE_ENTITY) {
            // Make sure an arm animation packet is sent directly after an attack as it is the next packet in the client
            // code.
            final WrapperPlayClientUseEntity useEntityWrapper = new WrapperPlayClientUseEntity(packetEvent.getPacket());
            if (useEntityWrapper.getType() == EnumWrappers.EntityUseAction.ATTACK) {
                user.getDataMap().setValue(DataKey.PACKET_ANALYSIS_ANIMATION_EXPECTED, true);
            }
        }
    }

    @Override
    public Set<ServerVersion> getSupportedVersions()
    {
        return ServerVersion.NON_188_VERSIONS;
    }

    @Override
    public boolean isSubModule()
    {
        return true;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.Animation";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.PACKET_ANALYSIS;
    }
}
