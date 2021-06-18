package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.DataKey;
import de.photon.aacadditionpro.util.messaging.DebugSender;
import lombok.Getter;

import java.util.function.ToIntFunction;

/**
 * This pattern detects bursts of sprinting while scaffolding. No legit is able to properly utilize sprinting so far
 * because of the direction limitations.
 */
class ScaffoldSprinting extends Module
{
    @Getter
    private ToIntFunction<User> applyingConsumer = user -> 0;

    public ScaffoldSprinting(String configString)
    {
        super(configString);
    }

    @Override
    public void enable()
    {
        applyingConsumer = user -> {
            if (user.hasSprintedRecently(400)) {
                if (user.getDataMap().getCounter(DataKey.CounterKey.SCAFFOLD_SPRINTING_FAILS).incrementCompareThreshold()) {
                    DebugSender.getInstance().sendDebug("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " sprinted suspiciously.");
                    return 8;
                }
            } else user.getDataMap().getCounter(DataKey.CounterKey.SCAFFOLD_SPRINTING_FAILS).decrementAboveZero();
            return 0;
        };
    }

    @Override
    public void disable()
    {
        applyingConsumer = user -> 0;
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this).build();
    }
}
