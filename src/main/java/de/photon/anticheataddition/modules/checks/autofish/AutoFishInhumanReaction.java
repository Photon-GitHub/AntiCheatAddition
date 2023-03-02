package de.photon.anticheataddition.modules.checks.autofish;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.mathematics.Polynomial;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

public final class AutoFishInhumanReaction extends ViolationModule implements Listener
{
    public static final AutoFishInhumanReaction INSTANCE = new AutoFishInhumanReaction();

    private static final Polynomial VL_CALCULATOR = new Polynomial(-60, 60);
    private final int cancelVl = AntiCheatAddition.getInstance().getConfig().getInt("AutoFish.cancel_vl");

    private final double humanReactionTime = loadDouble(".human_reaction_time", 145);

    private AutoFishInhumanReaction()
    {
        super("AutoFish.parts.inhuman_reaction");
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event)
    {
        final var user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        switch (event.getState()) {
            case CAUGHT_FISH -> {
                // Too few time has passed since the fish bit.
                final long passedBiteTime = user.getTimeMap().at(TimeKey.FISH_BITE).passedTime();
                final int vl = VL_CALCULATOR.apply(passedBiteTime / humanReactionTime).intValue();
                if (vl > 0) {
                    // Flag for vl = b + 1 because there would otherwise be a "0-vl"
                    this.getManagement().flag(Flag.of(user)
                                                  .setAddedVl(vl)
                                                  .setDebug(() -> "AutoFish-Debug | Player " + user.getPlayer().getName() + " failed inhuman reaction")
                                                  .setCancelAction(cancelVl, () -> event.setCancelled(true)));
                }

                // Reset the bite-timestamp to be ready for the next one
                user.getTimeMap().at(TimeKey.FISH_BITE).setToZero();
            }
            case BITE -> user.getTimeMap().at(TimeKey.FISH_BITE).update();
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
