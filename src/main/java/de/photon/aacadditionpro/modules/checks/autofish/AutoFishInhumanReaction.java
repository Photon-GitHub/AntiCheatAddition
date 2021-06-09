package de.photon.aacadditionpro.modules.checks.autofish;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.mathematics.Polynomial;
import de.photon.aacadditionpro.util.messaging.DebugSender;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

public class AutoFishInhumanReaction extends ViolationModule implements Listener
{
    private static final Polynomial VL_CALCULATOR = new Polynomial(-60, 60);
    private final int cancelVl = AACAdditionPro.getInstance().getConfig().getInt("AutoFish.cancel_vl");

    @LoadFromConfiguration(configPath = ".human_reaction_time")
    private double humanReactionTime;

    public AutoFishInhumanReaction()
    {
        super("AutoFish.parts.inhuman_reaction");
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event)
    {
        val user = User.getUser(event.getPlayer().getUniqueId());
        if (User.isUserInvalid(user, this)) return;

        switch (event.getState()) {
            case CAUGHT_FISH:
                // Too few time has passed since the fish bit.
                val passedBiteTime = user.getTimestampMap().at(TimestampKey.LAST_FISH_BITE).passedTime();
                val vl = VL_CALCULATOR.apply(passedBiteTime / humanReactionTime).intValue();

                if (vl > 0) {
                    DebugSender.getInstance().sendDebug("AutoFish-Verbose | Player " + user.getPlayer().getName() + " failed inhuman reaction");

                    // Flag for vl = b + 1 because there would otherwise be a "0-vl"
                    this.getManagement().flag(Flag.of(user)
                                                  .setAddedVl(vl)
                                                  .setCancelAction(cancelVl, () -> event.setCancelled(true)));
                }

                // Reset the bite-timestamp to be ready for the next one
                user.getTimestampMap().at(TimestampKey.LAST_FISH_BITE).setToZero();
                break;
            case BITE:
                user.getTimestampMap().at(TimestampKey.LAST_FISH_BITE).update();
                break;
            default:
                break;
        }
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .addAllowedServerVersions(ServerVersion.NON_188_VERSIONS)
                           .build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).withDecay(600, 5).build();
    }
}
