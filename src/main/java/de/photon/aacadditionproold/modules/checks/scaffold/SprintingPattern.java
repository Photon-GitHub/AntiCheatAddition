package de.photon.aacadditionproold.modules.checks.scaffold;

import de.photon.aacadditionproold.modules.Module;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.user.User;
import de.photon.aacadditionproold.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionproold.util.messaging.VerboseSender;
import lombok.Getter;

import java.util.function.ToIntFunction;

/**
 * This pattern detects bursts of sprinting while scaffolding. No legit is able to properly utilize sprinting so far
 * because of the direction limitations.
 */
class SprintingPattern implements Module
{
    @Getter
    private static final SprintingPattern instance = new SprintingPattern();

    @LoadFromConfiguration(configPath = ".violation_threshold")
    private int violationThreshold;

    @Getter
    private ToIntFunction<User> applyingConsumer = user -> 0;

    @Override
    public void enable()
    {
        applyingConsumer = user -> {
            if (user.hasSprintedRecently(400)) {
                if (++user.getScaffoldData().sprintingFails >= this.violationThreshold) {
                    VerboseSender.getInstance().sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " sprinted suspiciously.");
                    // Flag the player
                    return 8;
                }
            } else if (user.getScaffoldData().sprintingFails > 0) {
                --user.getScaffoldData().sprintingFails;
            }

            return 0;
        };
    }

    @Override
    public void disable()
    {
        applyingConsumer = user -> 0;
    }

    @Override
    public boolean isSubModule()
    {
        return true;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.sprinting";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.SCAFFOLD;
    }
}
