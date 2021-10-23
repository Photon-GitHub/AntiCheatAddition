package de.photon.aacadditionpro.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.Module;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

@EqualsAndHashCode(callSuper = true)
public class ModulePacketAdapter extends PacketAdapter
{
    @Getter
    @NotNull private final Module module;

    private ModulePacketAdapter(@NotNull Module module, ListenerPriority listenerPriority, PacketType[] types)
    {
        super(AACAdditionPro.getInstance(), listenerPriority, types);
        this.module = module;
    }

    public static Builder of(@NotNull Module module, @NotNull PacketType... types)
    {
        Preconditions.checkNotNull(module, "Tried to create ModulePacketAdapter for null module");
        Preconditions.checkNotNull(types, "Tried to create ModulePacketAdapter for null packet types");
        Preconditions.checkArgument(types.length > 0, "Tried to create ModulePacketAdapter for no packet types.");
        return new Builder(module, types);
    }

    @RequiredArgsConstructor
    public static class Builder
    {
        @NotNull private final Module module;
        @NotNull private final PacketType[] types;

        private ListenerPriority priority = ListenerPriority.NORMAL;
        private Consumer<PacketEvent> onReceiving = null;
        private Consumer<PacketEvent> onSending = null;

        public Builder priority(ListenerPriority priority)
        {
            Preconditions.checkNotNull(priority, "Tried to set ModulePacketAdapter ListenerPriority to null.");
            this.priority = priority;
            return this;
        }

        public Builder onReceiving(Consumer<PacketEvent> onReceiving)
        {
            this.onReceiving = onReceiving;
            return this;
        }

        public Builder onSending(Consumer<PacketEvent> onSending)
        {
            this.onSending = onSending;
            return this;
        }

        public ModulePacketAdapter build()
        {
            Preconditions.checkArgument(this.onReceiving != null || this.onSending != null, "Tried to create PacketAdapter without receiving or sending actions.");

            if (this.onReceiving != null && this.onSending != null) {
                return new ModulePacketAdapter(this.module, this.priority, this.types)
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
                return new ModulePacketAdapter(this.module, this.priority, this.types)
                {
                    @Override
                    public void onPacketReceiving(PacketEvent event)
                    {
                        onReceiving.accept(event);
                    }
                };
            } else {
                return new ModulePacketAdapter(this.module, this.priority, this.types)
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
}