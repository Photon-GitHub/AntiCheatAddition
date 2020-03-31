package de.photon.aacadditionpro.modules.clientcontrol;

import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PluginMessageListenerModule;
import de.photon.aacadditionpro.modules.RestrictedServerVersion;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.olduser.UserOld;
import de.photon.aacadditionpro.util.pluginmessage.MessageChannel;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.BitSet;
import java.util.Set;

public class FiveZigControl extends ClientControlModule implements PluginMessageListenerModule, RestrictedServerVersion
{
    // Backup: Channel name has to be EXACTLY "5zig_Set"
    private static final MessageChannel FIVEZIGCHANNEL = new MessageChannel("5zig", "set", "5zig_Set");

    /**
     * This depicts what features of 5zig are allowed
     * <p>
     * [0] | potion effect hud <br>
     * [1] | potion indicator vignette <br>
     * [2] | armor hud <br>
     * [3] | display player saturation <br>
     * [4] | entity health indicator <br>
     * [5] | auto reconnect
     */
    private final boolean[] features = new boolean[6];

    @Override
    public void onPluginMessageReceived(@NotNull final String channel, final Player player, @NotNull final byte[] message)
    {
        final UserOld user = UserManager.getUser(player.getUniqueId());

        if (UserOld.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        // ------------------------------------------------ 5zig confirmed -------------------------------------------- //

        // Bypassed players are already filtered out.
        // The mod provides a method to disable parts of it
        final BitSet disableBitSet = new BitSet();

        // Set the according bits
        for (byte b = 0; b < features.length; b++) {
            disableBitSet.set(b, features[b]);
        }

        user.getPlayer().sendPluginMessage(AACAdditionPro.getInstance(), FIVEZIGCHANNEL.getChannel(), disableBitSet.toByteArray());
        executeCommands(user.getPlayer());

        // ------------------------------------------------ 5zig end -------------------------------------------- //

    }

    @Override
    public void enable()
    {
        features[0] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.potion_effect_hud");
        features[1] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.potion_indicator_vignette");
        features[2] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.armour_hud");
        features[3] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.player_saturation");
        features[4] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.entity_health_indicator");
        features[5] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.auto_reconnect");
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.FIVEZIG_CONTROL;
    }

    @Override
    public Set<MessageChannel> getIncomingChannels()
    {
        return ImmutableSet.of(FIVEZIGCHANNEL);
    }

    @Override
    public Set<MessageChannel> getOutgoingChannels()
    {
        return ImmutableSet.of(FIVEZIGCHANNEL);
    }

    @Override
    public Set<ServerVersion> getSupportedVersions()
    {
        return ServerVersion.LEGACY_PLUGIN_MESSAGE_VERSIONS;
    }
}
