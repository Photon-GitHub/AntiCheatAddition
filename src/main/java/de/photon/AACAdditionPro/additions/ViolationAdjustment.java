package de.photon.AACAdditionPro.additions;

import de.photon.AACAdditionPro.Module;
import de.photon.AACAdditionPro.ModuleType;
import me.konsolas.aac.api.PlayerViolationEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ViolationAdjustment implements Module, Listener
{
    @EventHandler
    public void onAACViolation(PlayerViolationEvent event)
    {
        // TODO: Let's get started.
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.VIOLATION_ADJUSTMENT;
    }
}
