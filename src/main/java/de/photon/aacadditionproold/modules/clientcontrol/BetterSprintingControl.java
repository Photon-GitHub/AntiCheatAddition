package de.photon.aacadditionproold.modules.clientcontrol;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionproold.AACAdditionPro;
import de.photon.aacadditionproold.ServerVersion;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.modules.PluginMessageListenerModule;
import de.photon.aacadditionproold.modules.RestrictedServerVersion;
import de.photon.aacadditionproold.user.User;
import de.photon.aacadditionproold.user.UserManager;
import de.photon.aacadditionproold.util.exceptions.UnknownMinecraftVersion;
import de.photon.aacadditionproold.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionproold.util.pluginmessage.MessageChannel;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class BetterSprintingControl extends ClientControlModule implements PluginMessageListenerModule, RestrictedServerVersion
{
    @Getter
    private static final BetterSprintingControl instance = new BetterSprintingControl();

    private static final Set<MessageChannel> CHANNELS;

    static {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
            case MC112:
                CHANNELS = ImmutableSet.of(new MessageChannel("minecraft", "bsm", "BSM"),
                                           new MessageChannel("minecraft", "bsprint", "BSprint"));
                break;
            case MC113:
            case MC114:
            case MC115:
            case MC116:
                CHANNELS = ImmutableSet.of(new MessageChannel("bsm", "settings"));
                break;
            default:
                throw new UnknownMinecraftVersion();
        }
    }


    private final List<Boolean> featureList = ImmutableList.of(!AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.survival_fly_boost"),
                                                               !AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.enable_all_dirs"));

    @LoadFromConfiguration(configPath = ".disable_mod")
    private boolean disable;

    @Override
    public void onPluginMessageReceived(@NotNull final String channel, final Player player, @NotNull final byte[] message)
    {
        final User user = UserManager.getUser(player.getUniqueId());

        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
            case MC112:
                // Bypassed players are already filtered out.
                // The mod provides a method to disable it
                if (disable) {
                    // The channel is always BSM, the right one.
                    user.getPlayer().sendPluginMessage(AACAdditionPro.getInstance(), channel, new byte[]{1});
                }
                break;
            case MC113:
            case MC114:
            case MC115:
            case MC116:
                final ByteBuf settingsBuf = Unpooled.buffer();
                settingsBuf.writeByte(0);
                for (Boolean enable : featureList) {
                    settingsBuf.writeBoolean(enable);
                }

                user.getPlayer().sendPluginMessage(AACAdditionPro.getInstance(), channel, settingsBuf.array());

                final ByteBuf disableBuf = Unpooled.buffer();
                // Bypassed players are already filtered out.
                // The mod provides a method to disable it
                disableBuf.writeByte(disable ? 1 : 2);
                user.getPlayer().sendPluginMessage(AACAdditionPro.getInstance(), channel, disableBuf.array());
                break;
            default:
                throw new UnknownMinecraftVersion();
        }


        executeCommands(user.getPlayer());
    }

    @Override
    public Set<MessageChannel> getIncomingChannels()
    {
        return CHANNELS;
    }

    @Override
    public Set<MessageChannel> getOutgoingChannels()
    {
        return CHANNELS;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.BETTERSPRINTING_CONTROL;
    }

    @Override
    public Set<ServerVersion> getSupportedVersions()
    {
        return ServerVersion.ALL_SUPPORTED_VERSIONS;
    }
}
