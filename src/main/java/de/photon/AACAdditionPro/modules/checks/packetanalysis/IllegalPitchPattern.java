package de.photon.AACAdditionPro.modules.checks.packetanalysis;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import de.photon.AACAdditionPro.util.packetwrappers.client.IWrapperPlayClientLook;

public class IllegalPitchPattern extends PatternModule.PacketPattern
{
    protected IllegalPitchPattern()
    {
        super(ImmutableSet.of(PacketType.Play.Client.LOOK, PacketType.Play.Client.POSITION_LOOK));
    }

    @Override
    protected int process(User user, PacketEvent packetEvent)
    {
        final IWrapperPlayClientLook lookWrapper = packetEvent::getPacket;
        return MathUtils.inRange(-90, 90, lookWrapper.getPitch()) ? 0 : 20;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.IllegalPitch";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.PACKET_ANALYSIS;
    }
}
