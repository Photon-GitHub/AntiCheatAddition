package de.photon.AACAdditionPro.modules.checks.autofish;

import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import org.bukkit.event.player.PlayerFishEvent;

class InhumanReactionPattern extends PatternModule.Pattern<User, PlayerFishEvent>
{
    @LoadFromConfiguration(configPath = ".parts.inhuman_reaction.fishing_milliseconds")
    private int fishing_milliseconds;

    @Override
    protected int process(User user, PlayerFishEvent event)
    {
        switch (event.getState())
        {
            case CAUGHT_FISH:
                // Too few time has passed since the fish bit.
                if (user.getFishingData().recentlyUpdated(0, fishing_milliseconds))
                {

                    // Get the correct amount of vl.
                    // vl 6 is the maximum.
                    // Points = {{0, 1}, {8, 0}}
                    // Function: 1 - 0.125x
                    for (byte b = 5; b > 0; b--)
                    {
                        if (user.getFishingData().recentlyUpdated(0, (long) (1 - 0.125 * b) * fishing_milliseconds))
                        {
                            // Flag for vl = b + 1 because there would otherwise be a "0-vl"
                            VerboseSender.getInstance().sendVerboseMessage("AutoFish-Verbose | Player " + user.getPlayer().getName() + " failed inhuman reaction");
                            return b + 1;
                        }
                    }
                }

                // Reset the bite-timestamp to be ready for the next one
                user.getFishingData().nullifyTimeStamp(0);
                break;
            case BITE:
                user.getFishingData().updateTimeStamp(0);
                break;
            default:
                break;
        }
        return 0;
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
