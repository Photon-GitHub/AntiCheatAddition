package de.photon.aacadditionpro.modules.checks.autofish;

import de.photon.aacadditionpro.modules.ListenerModule;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.user.TimestampKey;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.messaging.VerboseSender;
import lombok.Getter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerFishEvent;

class InhumanReactionPattern implements ListenerModule
{
    @Getter
    private static final InhumanReactionPattern instance = new InhumanReactionPattern();

    @LoadFromConfiguration(configPath = ".fishing_milliseconds")
    private int fishingMilliseconds;

    @EventHandler(ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // User valid and not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        switch (event.getState()) {
            case CAUGHT_FISH:
                // Too few time has passed since the fish bit.
                if (user.getTimestampMap().recentlyUpdated(TimestampKey.LAST_FISH_BITE, fishingMilliseconds)) {

                    // Get the correct amount of vl.
                    // vl 6 is the maximum.
                    // Points = {{0, 1}, {8, 0}}
                    // Function: 1 - 0.125x
                    for (byte b = 5; b > 0; b--) {
                        if (user.getTimestampMap().recentlyUpdated(TimestampKey.LAST_FISH_BITE, (long) (1 - 0.125 * b) * fishingMilliseconds)) {
                            VerboseSender.getInstance().sendVerboseMessage("AutoFish-Verbose | Player " + user.getPlayer().getName() + " failed inhuman reaction");

                            // Flag for vl = b + 1 because there would otherwise be a "0-vl"
                            AutoFish.getInstance().getViolationLevelManagement().flag(event.getPlayer(),
                                                                                      (int) b + 1,
                                                                                      AutoFish.getInstance().getCancelVl(),
                                                                                      () -> event.setCancelled(true), () -> {});
                        }
                    }
                }

                // Reset the bite-timestamp to be ready for the next one
                user.getTimestampMap().nullifyTimeStamp(TimestampKey.LAST_FISH_BITE);
                break;
            case BITE:
                user.getTimestampMap().updateTimeStamp(TimestampKey.LAST_FISH_BITE);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean isSubModule()
    {
        return true;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.inhuman_reaction";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.AUTO_FISH;
    }
}
