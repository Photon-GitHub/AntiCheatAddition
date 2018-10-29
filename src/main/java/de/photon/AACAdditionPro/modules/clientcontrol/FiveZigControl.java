package de.photon.AACAdditionPro.modules.clientcontrol;

import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ServerVersion;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PluginMessageListenerModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import org.bukkit.entity.Player;

import java.util.BitSet;
import java.util.Set;

public class FiveZigControl extends ClientControlModule implements PluginMessageListenerModule
{
    // Backup: Channel name has to be EXACTLY "5zig_Set"
    private static final String FIVEZIGCHANNEL = ServerVersion.LEGACY_PLUGIN_MESSAGE_VERSIONS.contains(ServerVersion.getActiveServerVersion()) ?
                                                 "5zig_Set" :
                                                 "minecraft:5zig_Set";

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
    public void onPluginMessageReceived(final String channel, final Player player, final byte[] message)
    {
        final User user = UserManager.getUser(player.getUniqueId());

        if (User.isUserInvalid(user, this.getModuleType())) {
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

        user.getPlayer().sendPluginMessage(AACAdditionPro.getInstance(), FIVEZIGCHANNEL, disableBitSet.toByteArray());
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
    public Set<String> getLegacyPluginMessageChannels()
    {
        return ImmutableSet.of(FIVEZIGCHANNEL);
    }

    @Override
    public Set<String> getPluginMessageChannels()
    {
        return ImmutableSet.of(FIVEZIGCHANNEL);
    }
}
