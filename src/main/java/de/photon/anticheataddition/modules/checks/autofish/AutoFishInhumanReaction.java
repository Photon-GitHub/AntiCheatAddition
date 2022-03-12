package de.photon.anticheataddition.modules.checks.autofish;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimestampKey;
import de.photon.anticheataddition.util.config.LoadFromConfiguration;
import de.photon.anticheataddition.util.mathematics.Polynomial;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

public class AutoFishInhumanReaction extends ViolationModule implements Listener
{
    private static final Polynomial VL_CALCULATOR = new Polynomial(-60, 60);
    private final int cancelVl = AntiCheatAddition.getInstance().getConfig().getInt("AutoFish.cancel_vl");

    @LoadFromConfiguration(configPath = ".human_reaction_time")
    private double humanReactionTime;

    public AutoFishInhumanReaction()
    {
        super("AutoFish.parts.inhuman_reaction");
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event)
    {
        val user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        switch (event.getState()) {
            case CAUGHT_FISH:
                // Too few time has passed since the fish bit.
                val passedBiteTime = user.getTimestampMap().at(TimestampKey.LAST_FISH_BITE).passedTime();
                val vl = VL_CALCULATOR.apply(passedBiteTime / humanReactionTime).intValue();

                if (vl > 0) {
                    // Flag for vl = b + 1 because there would otherwise be a "0-vl"
                    this.getManagement().flag(Flag.of(user)
                                                  .setAddedVl(vl)
                                                  .setDebug("AutoFish-Debug | Player " + user.getPlayer().getName() + " failed inhuman reaction")
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
                           .setAllowedServerVersions(ServerVersion.NON_188_VERSIONS)
                           .build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(800, 3).build();
    }
}
