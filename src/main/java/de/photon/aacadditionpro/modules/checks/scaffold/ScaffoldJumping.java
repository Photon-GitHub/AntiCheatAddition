package de.photon.aacadditionpro.modules.checks.scaffold;


import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.DataKey;
import de.photon.aacadditionpro.user.data.TimestampKey;
import lombok.Getter;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.function.ToIntBiFunction;

/**
 * This pattern checks for suspicious positions when placing a block to prevent extend scaffolds.
 */
class ScaffoldJumping extends Module
{
    @Getter
    private ToIntBiFunction<User, BlockPlaceEvent> applyingConsumer = (user, event) -> 0;

    public ScaffoldJumping(String scaffoldConfigString)
    {
        super(scaffoldConfigString + ".parts.Jumping");
    }

    @Override
    public void enable()
    {
        applyingConsumer = (user, event) ->
                user.hasMovedRecently(TimestampKey.LAST_XZ_MOVEMENT, 500)
                && user.getTimestampMap().at(TimestampKey.LAST_VELOCITY_CHANGE_NO_EXTERNAL_CAUSES).recentlyUpdated(1000)
                && user.getDataMap().getCounter(DataKey.CounterKey.SCAFFOLD_JUMPING_FAILS).incrementCompareThreshold() ? 20 : 0;
    }

    @Override
    public void disable()
    {
        applyingConsumer = (user, event) -> 0;
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this).build();
    }
}
