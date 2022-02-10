package de.photon.aacadditionpro.modules.sentinel;

import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.config.ConfigUtils;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.pluginmessage.MessageChannel;
import lombok.val;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class SentinelChannelModule extends SentinelModule implements PluginMessageListener
{
    @LoadFromConfiguration(configPath = ".containsAll")
    private List<String> containsAll;

    @LoadFromConfiguration(configPath = ".containsAll")
    private List<String> containsAny;

    public SentinelChannelModule(String configString)
    {
        super(configString);
    }

    @Override
    public void onPluginMessageReceived(@NotNull final String channel, @NotNull final Player player, final byte[] message)
    {
        val user = User.getUser(player);
        if (User.isUserInvalid(user, this)) return;

        val stringMessage = new String(message);
        if (!containsAll.isEmpty() && !containsAll.stream().allMatch(stringMessage::contains)) return;
        if (!containsAny.isEmpty() && containsAny.stream().noneMatch(stringMessage::contains)) return;

        this.detection(user.getPlayer());
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
