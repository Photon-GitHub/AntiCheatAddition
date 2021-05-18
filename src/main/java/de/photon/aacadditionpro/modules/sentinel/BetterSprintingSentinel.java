package de.photon.aacadditionpro.modules.sentinel;

import com.google.common.collect.ImmutableList;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.pluginmessage.MessageChannel;
import io.netty.buffer.Unpooled;
import lombok.val;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BetterSprintingSentinel extends SentinelModule
{
    private final List<Boolean> featureList = ImmutableList.of(!AACAdditionPro.getInstance().getConfig().getBoolean(this.getConfigString() + ".disable.survival_fly_boost"),
                                                               !AACAdditionPro.getInstance().getConfig().getBoolean(this.getConfigString() + ".disable.enable_all_dirs"));
    @LoadFromConfiguration(configPath = ".disable_mod")
    private boolean disable;

    public BetterSprintingSentinel()
    {
        super("BetterSprinting");
    }

    @Override
    public void onPluginMessageReceived(@NotNull final String channel, final Player player, @NotNull final byte[] message)
    {
        val user = User.getUser(player.getUniqueId());
        if (User.isUserInvalid(user, this)) return;

        detection(user.getPlayer());

        // API_DOCS of BetterSprinting:
        /*
         * OUTCOMING PACKETS
         * =================
         *
         *** [byte 0] [bool <enableSurvivalFlyBoost>] [bool <enableAllDirs>]
         ***
         *** Notifies the player about which non-vanilla settings are enabled on the server (both are disabled by default).
         *** Sent to player when their [byte 0] message is processed, and either or both settings are enabled.
         *** Sent to all players with the mod after using the '/bettersprinting setting (...)' command.
         *
         *
         *** [byte 1]
         ***
         *** Disables basic functionality of the mod on client side.
         *** Sent to player when their [byte 0] message is processed, and the server wants to disable the mod.
         *** Sent to all players with the mod after using the '/bettersprinting disablemod true' command.
         *
         *
         *** [byte 2]
         ***
         *** Re-enables basic functionality of the mod on client side.
         *** Sent to all players with the mod after using the '/bettersprinting disablemod false' command.
         */
        final String sendChannel;
        switch (channel) {
            case "BSprint":
            case "BSM":
                sendChannel = "BSM";
                break;
            case "bsm:settings":
                sendChannel = "bsm:settings";
                break;
            default:
                throw new IllegalStateException("Unknown channel");
        }

        val settingsBuf = Unpooled.buffer();
        settingsBuf.writeByte(0);

        for (Boolean enable : featureList) settingsBuf.writeBoolean(enable);
        user.getPlayer().sendPluginMessage(AACAdditionPro.getInstance(), sendChannel, settingsBuf.array());

        val disableBuf = Unpooled.buffer();
        // Bypassed players are already filtered out.
        // The mod provides a method to disable it
        disableBuf.writeByte(disable ? 1 : 2);
        user.getPlayer().sendPluginMessage(AACAdditionPro.getInstance(), sendChannel, disableBuf.array());
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .addIncomingMessageChannels(MessageChannel.of("minecraft", "bsm", "BSM"),
                                                       MessageChannel.of("minecraft", "bsprint", "BSprint"),
                                                       MessageChannel.of("bsm", "settings"))
                           // No message is sent in BSprint.
                           .addOutgoingMessageChannels(MessageChannel.of("minecraft", "bsm", "BSM"),
                                                       MessageChannel.of("bsm", "settings"))
                           .build();
    }
}
