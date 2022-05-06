package de.photon.anticheataddition.modules.sentinel;

import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.pluginmessage.MessageChannel;
import lombok.val;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SentinelChannelModule extends SentinelModule implements ParsedPluginMessageListener
{
    private final List<String> containsAll = loadStringList(".containsAll");
    private final List<String> containsAny = loadStringList(".containsAny");

    public SentinelChannelModule(String configString)
    {
        super(configString);
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull String message)
    {
        // If containsAll or containsAny is empty, skip the respective check.
        if ((containsAll.isEmpty() || containsAll.stream().allMatch(message::contains)) &&
            (containsAny.isEmpty() || containsAny.stream().anyMatch(message::contains)))
        {
            detection(player);
        }
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        val builder = ModuleLoader.builder(this);
        loadStringList(".incoming_channels").stream().map(MessageChannel::of).forEach(builder::addIncomingMessageChannel);
        // No outgoing channels as the config does not allow for sending in a channel.
        return builder.build();
    }
}
