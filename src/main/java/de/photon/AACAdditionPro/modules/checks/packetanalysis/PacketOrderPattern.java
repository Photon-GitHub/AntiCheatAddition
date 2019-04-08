package de.photon.AACAdditionPro.modules.checks.packetanalysis;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.util.packetwrappers.client.WrapperPlayClientEntityAction;
import de.photon.AACAdditionPro.util.packetwrappers.client.WrapperPlayClientUseEntity;

public class PacketOrderPattern extends PatternModule.PacketPattern
{

    PacketOrderPattern()
    {
        // THIS IS IN ORDER OF HOW THE PACKETS ARE SUPPOSED TO ARRIVE.
        super(ImmutableSet.of(PacketType.Play.Client.USE_ENTITY,
                              PacketType.Play.Client.ARM_ANIMATION,
                              // If sneaking, must be sent before the movement packets
                              // Otherwise:
                              PacketType.Play.Client.ENTITY_ACTION,
                              PacketType.Play.Client.USE_ITEM,
                              // One of the next 4 must be sent exactly once per tick
                              PacketType.Play.Client.LOOK,
                              PacketType.Play.Client.POSITION,
                              PacketType.Play.Client.POSITION_LOOK,
                              PacketType.Play.Client.FLYING));
    }

    @Override
    protected int process(User user, PacketEvent packetEvent)
    {
        if (packetEvent.getPacketType() == PacketType.Play.Client.ARM_ANIMATION) {
            System.out.println("ARM ANIMATION");

            user.getPacketAnalysisData().animationExpected = false;
        }
        else {
            if (user.getPacketAnalysisData().animationExpected) {
                user.getPacketAnalysisData().animationExpected = false;
                message = "PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " did not send animation packet after an attack.";
                return 10;
            }
        }

        if (packetEvent.getPacketType() == PacketType.Play.Client.USE_ENTITY) {
            System.out.println("USE ENTITY");

            // Make sure an arm animation packet is sent directly after an attack as it is the next packet in the client
            // code.
            final WrapperPlayClientUseEntity useEntityWrapper = new WrapperPlayClientUseEntity(packetEvent.getPacket());
            if (useEntityWrapper.getType() == EnumWrappers.EntityUseAction.ATTACK) {
                user.getPacketAnalysisData().animationExpected = true;
            }
            return 0;
        }

        if (packetEvent.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            System.out.println("ACTION");
            final WrapperPlayClientEntityAction actionWrapper = new WrapperPlayClientEntityAction(packetEvent.getPacket());
            return 0;
        }

        if (packetEvent.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            System.out.println("USE ITEM");
            return 0;
        }

        if (packetEvent.getPacketType() == PacketType.Play.Client.LOOK ||
            packetEvent.getPacketType() == PacketType.Play.Client.POSITION ||
            packetEvent.getPacketType() == PacketType.Play.Client.POSITION_LOOK)
        {
            System.out.println("POSLOOK");
            return 0;
        }

        if (packetEvent.getPacketType() == PacketType.Play.Client.FLYING) {
            System.out.println("FLYING");
            return 0;
        }

        return 0;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.CombatOrder";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.PACKET_ANALYSIS;
    }
}
