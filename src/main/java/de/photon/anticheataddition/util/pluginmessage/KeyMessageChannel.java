package de.photon.anticheataddition.util.pluginmessage;

import com.comphenix.protocol.wrappers.MinecraftKey;
import com.google.common.base.Preconditions;
import de.photon.anticheataddition.ServerVersion;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public final class KeyMessageChannel extends MinecraftKey implements MessageChannel
{
    KeyMessageChannel(@NotNull String prefix, @NotNull String key)
    {
        super(Preconditions.checkNotNull(prefix, "Tried to create KeyMessageChannel with null prefix."), Preconditions.checkNotNull(key, "Tried to create KeyMessageChannel with null key."));
        Preconditions.checkState(!ServerVersion.containsActive(ServerVersion.LEGACY_PLUGIN_MESSAGE_VERSIONS), "Tried to create KeyMessageChannel on old version.");
    }

    @Override
    public Optional<String> getChannel()
    {
        return Optional.of(this.getFullKey());
    }

    @Override
    public boolean equals(Object o)
    {
        // Manual equals() and hashCode() as we need to access the super class which does not specify them.
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeyMessageChannel that = (KeyMessageChannel) o;
        return Objects.equals(this.getPrefix(), that.getPrefix()) && Objects.equals(this.getKey(), that.getKey());
    }

    @Override
    public int hashCode()
    {
        // Manual equals() and hashCode() as we need to access the super class which does not specify them.
        return Objects.hash(this.getPrefix(), this.getKey());
    }
}
