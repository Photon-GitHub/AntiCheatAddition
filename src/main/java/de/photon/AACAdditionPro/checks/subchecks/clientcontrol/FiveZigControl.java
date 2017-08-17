package de.photon.AACAdditionPro.checks.subchecks.clientcontrol;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.checks.ClientControlCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.List;

public class FiveZigControl implements PluginMessageListener, ClientControlCheck
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

        if (AACAdditionProCheck.isUserInvalid(user)) {
            return;
        }

        try {
            // ------------------------------------------------ 5zig confirmed -------------------------------------------- //

            // Bypassed players are already filtered out.
            // The mod provides a method to disable parts of it
            byte disableByte = (byte) 0;

            // Set the according bits
            for (byte b = 0; b < features.length; b++) {
                if (features[b]) {
                    // --------------------------------------------------- //
                    // BE SURE THAT THIS CANNOT CAUSE THE BYTE TO OVERFLOW
                    // --------------------------------------------------- //

                    disableByte |= (0x01 << b);
                }
            }

            user.getPlayer().sendPluginMessage(AACAdditionPro.getInstance(), FIVEZIGCHANNEL, new byte[]{disableByte});
            executeCommands(user.getPlayer());

            // ------------------------------------------------ 5zig end -------------------------------------------- //
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<String> getCommandsOnDetection()
    {
        return commandsOnDetection;
    }

    @Override
    public AdditionHackType getAdditionHackType()
    {
        return AdditionHackType.FIVEZIG_CONTROL;
    }

    @Override
    public void subEnable()
    {
        features[0] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getAdditionHackType().getConfigString() + ".disable.potion_effect_hud");
        features[1] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getAdditionHackType().getConfigString() + ".disable.potion_indicator_vignette");
        features[2] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getAdditionHackType().getConfigString() + ".disable.armour_hud");
        features[3] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getAdditionHackType().getConfigString() + ".disable.player_saturation");
        features[4] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getAdditionHackType().getConfigString() + ".disable.entity_health_indicator");
        features[5] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getAdditionHackType().getConfigString() + ".disable.auto_reconnect");
    }

    @Override
    public String[] getPluginMessageChannels()
    {
        return new String[]{FIVEZIGCHANNEL};
    }
}
