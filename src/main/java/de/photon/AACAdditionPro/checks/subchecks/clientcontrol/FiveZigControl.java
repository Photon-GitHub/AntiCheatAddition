package de.photon.AACAdditionPro.checks.subchecks.clientcontrol;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ClientControlModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.BitSet;
import java.util.List;

public class FiveZigControl implements PluginMessageListener, ClientControlModule
{
    // Backup: Channel name has to be EXACTLY "5zig_Set"
    private static final String FIVEZIGCHANNEL = "5zig_Set";

    @LoadFromConfiguration(configPath = ".commands_on_detection", listType = String.class)
    private List<String> commandsOnDetection;

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

        if (User.isUserInvalid(user))
        {
            return;
        }

        // ------------------------------------------------ 5zig confirmed -------------------------------------------- //

        // Bypassed players are already filtered out.
        // The mod provides a method to disable parts of it
        final BitSet disableBitSet = new BitSet();

        // Set the according bits
        for (byte b = 0; b < features.length; b++)
        {
            disableBitSet.set(b, features[b]);
        }

        user.getPlayer().sendPluginMessage(AACAdditionPro.getInstance(), FIVEZIGCHANNEL, disableBitSet.toByteArray());
        executeCommands(user.getPlayer());

        // ------------------------------------------------ 5zig end -------------------------------------------- //

    }

    @Override
    public List<String> getCommandsOnDetection()
    {
        return commandsOnDetection;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.FIVEZIG_CONTROL;
    }

    @Override
    public void subEnable()
    {
        features[0] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.potion_effect_hud");
        features[1] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.potion_indicator_vignette");
        features[2] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.armour_hud");
        features[3] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.player_saturation");
        features[4] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.entity_health_indicator");
        features[5] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.auto_reconnect");
    }

    @Override
    public String[] getPluginMessageChannels()
    {
        return new String[]{FIVEZIGCHANNEL};
    }
}
