package de.photon.AACAdditionPro.checks.subchecks.clientcontrol;

import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ClientControlModule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.List;

public class VersionControl implements Listener, ClientControlModule
{
    @EventHandler
    // TODO: ASYNCPLAYERPRELOGINEVENT OR PLAYERLOGINEVENT ?
    public void on(AsyncPlayerPreLoginEvent event)
    {
    }

    @Override
    public List<String> getCommandsOnDetection()
    {
        return null;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.VERSION_CONTROL;
    }
}
