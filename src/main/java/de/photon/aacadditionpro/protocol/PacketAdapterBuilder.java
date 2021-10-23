package de.photon.aacadditionpro.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.AACAdditionPro;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

@RequiredArgsConstructor
public class PacketAdapterBuilder
{
    @NotNull private final PacketType[] types;

    private ListenerPriority priority = ListenerPriority.NORMAL;
    private Consumer<PacketEvent> onReceiving = null;
    private Consumer<PacketEvent> onSending = null;

    public static PacketAdapterBuilder of(@NotNull PacketType... types)
    {
        Preconditions.checkNotNull(types, "Tried to create PacketAdapterBuilder with null types.");
        Preconditions.checkArgument(types.length > 0, "Tried to create PacketAdapterBuilder without types.");
        return new PacketAdapterBuilder(types);
    }

    public PacketAdapterBuilder priority(ListenerPriority priority)
    {
        Preconditions.checkNotNull(priority, "Tried to set PacketAdapterBuilder ListenerPriority to null.");
        this.priority = priority;
        return this;
    }

    public PacketAdapterBuilder onReceiving(Consumer<PacketEvent> onReceiving)
    {
        this.onReceiving = onReceiving;
        return this;
    }

    public PacketAdapterBuilder onSending(Consumer<PacketEvent> onSending)
    {
        this.onSending = onSending;
        return this;
    }

    public PacketAdapter build()
    {
        Preconditions.checkArgument(this.onReceiving != null || this.onSending != null, "Tried to create PacketAdapter without receiving or sending actions.");

        if (this.onReceiving != null && this.onSending != null) {
            return new PacketAdapter(AACAdditionPro.getInstance(), this.priority, this.types)
            {
                @Override
                public void onPacketReceiving(PacketEvent event)
                {
                    onReceiving.accept(event);
                }

                @Override
                public void onPacketSending(PacketEvent event)
                {
                    onSending.accept(event);
                }
            };
        } else if (onReceiving != null) {
            return new PacketAdapter(AACAdditionPro.getInstance(), this.priority, this.types)
            {
                @Override
                public void onPacketReceiving(PacketEvent event)
                {
                    onReceiving.accept(event);
                }
            };
        } else {
            return new PacketAdapter(AACAdditionPro.getInstance(), this.priority, this.types)
            {
                @Override
                public void onPacketSending(PacketEvent event)
                {
                    onSending.accept(event);
                }
            };
        }
    }
}