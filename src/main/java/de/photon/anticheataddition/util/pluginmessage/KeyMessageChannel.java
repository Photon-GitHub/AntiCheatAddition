package de.photon.anticheataddition.util.pluginmessage;

import com.google.common.base.Preconditions;
import de.photon.anticheataddition.ServerVersion;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class KeyMessageChannel implements MessageChannel
{
    @EqualsAndHashCode.Include @NotNull private final String prefix;
    @EqualsAndHashCode.Include @NotNull private final String key;
    @NotNull private final String fullKey;

    KeyMessageChannel(@NotNull String prefix, @NotNull String key)
    {
        Preconditions.checkNotNull(prefix, "Tried to create KeyMessageChannel with null prefix.");
        Preconditions.checkNotNull(key, "Tried to create KeyMessageChannel with null key.");
        Preconditions.checkState(!ServerVersion.containsActive(ServerVersion.LEGACY_PLUGIN_MESSAGE_VERSIONS), "Tried to create KeyMessageChannel on old version.");

        this.prefix = prefix;
        this.key = key;
        this.fullKey = prefix + ':' + key;
    }

    @Override
    public Optional<String> getChannel()
    {
        return Optional.of(this.getFullKey());
    }
}
