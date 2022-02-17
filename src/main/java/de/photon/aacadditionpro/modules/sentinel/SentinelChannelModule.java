package de.photon.aacadditionpro.modules.sentinel;

import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.config.ConfigUtils;
import de.photon.aacadditionpro.util.pluginmessage.MessageChannel;
import lombok.val;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class SentinelChannelModule extends SentinelModule implements ParsedPluginMessageListener
{
    private final List<String> containsAll = ConfigUtils.loadImmutableStringOrStringList(this.configString + ".containsAll");
    private final List<String> containsAny = ConfigUtils.loadImmutableStringOrStringList(this.configString + ".containsAny");

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
        val incoming = ConfigUtils.loadImmutableStringOrStringList(this.configString + ".incoming_channels");
        val outgoing = ConfigUtils.loadImmutableStringOrStringList(this.configString + ".outgoing_channels");

        if (!incoming.isEmpty()) builder.addIncomingMessageChannels(incoming.stream().map(MessageChannel::of).collect(Collectors.toUnmodifiableSet()));
        if (!outgoing.isEmpty()) builder.addOutgoingMessageChannels(outgoing.stream().map(MessageChannel::of).collect(Collectors.toUnmodifiableSet()));
        return builder.build();
    }
}
