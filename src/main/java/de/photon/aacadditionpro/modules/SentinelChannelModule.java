package de.photon.aacadditionpro.modules;

import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.config.ConfigUtils;
import de.photon.aacadditionpro.util.pluginmessage.MessageChannel;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import de.photon.aacadditionpro.util.violationlevels.threshold.ThresholdManagement;
import lombok.val;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

public class SentinelChannelModule extends SentinelModule implements PluginMessageListener
{
    public SentinelChannelModule(String configString)
    {
        super(configString);
    }

    @Override
    public void onPluginMessageReceived(@NotNull final String channel, final Player player, @NotNull final byte[] message)
    {
        val user = User.getUser(player.getUniqueId());
        if (User.isUserInvalid(user, this)) return;
        this.getManagement().flag(Flag.of(user.getPlayer()));
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        val builder = ModuleLoader.builder(this);
        val incoming = ConfigUtils.loadImmutableStringOrStringList(this.configString + ".incoming");
        val outgoing = ConfigUtils.loadImmutableStringOrStringList(this.configString + ".outgoing");

        if (!incoming.isEmpty()) builder.addIncomingMessageChannels(incoming.stream().map(MessageChannel::of).collect(Collectors.toSet()));
        if (!outgoing.isEmpty()) builder.addOutgoingMessageChannels(outgoing.stream().map(MessageChannel::of).collect(Collectors.toSet()));
        return builder.build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        // -1 decay ticks to never decay.
        return new ViolationLevelManagement(this, ThresholdManagement.loadCommands(this.configString + ".commands"), -1);
    }
}
