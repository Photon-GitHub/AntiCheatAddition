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
        val user = User.getUser(player);
        if (User.isUserInvalid(user, this)) return;

        // If containsAll or containsAny is empty, skip the respective check.
        if ((containsAll.isEmpty() || containsAll.stream().allMatch(message::contains)) &&
            (containsAny.isEmpty() || containsAny.stream().anyMatch(message::contains)))
        {
            this.detection(user.getPlayer());
        }
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        val builder = ModuleLoader.builder(this);
        val incoming = loadStringList(".incoming_channels");
        val outgoing = loadStringList(".outgoing_channels");


        if (!incoming.isEmpty()) incoming.stream().map(MessageChannel::of).forEach(builder::addIncomingMessageChannel);
        if (!outgoing.isEmpty()) incoming.stream().map(MessageChannel::of).forEach(builder::addOutgoingMessageChannel);
        return builder.build();
    }
}
