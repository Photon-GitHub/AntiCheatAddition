package de.photon.aacadditionpro.modules.sentinel;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import de.photon.aacadditionpro.util.violationlevels.threshold.ThresholdManagement;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public abstract class SentinelModule extends ViolationModule implements PluginMessageListener
{
    private static final int AAC_FEATURE_SCORE = AACAdditionPro.getInstance().getConfig().getInt("Sentinel.AACFeatureScore");

    public SentinelModule(String restString)
    {
        super("Sentinel." + restString);
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        // -1 decay ticks to never decay.
        return ViolationLevelManagement.builder(this)
                                       .withCustomThresholdManagement(ThresholdManagement.loadCommands(this.configString + ".commands"))
                                       .build();
    }

    /**
     * Used to signal the detection of a player.
     */
    protected void detection(Player player)
    {
        this.getManagement().flag(Flag.of(player).setAddedVl(AAC_FEATURE_SCORE));
    }
}
