package de.photon.aacadditionpro.modules;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import de.photon.aacadditionpro.AACAdditionPro;
import lombok.AccessLevel;
import lombok.Getter;

public class ModulePacketAdapter extends PacketAdapter
{
    @Getter(AccessLevel.PROTECTED)
    private final Module module;

    public ModulePacketAdapter(Module module, ListenerPriority listenerPriority, PacketType... types)
    {
        super(AACAdditionPro.getInstance(), listenerPriority, types);
        this.module = module;
    }
}
