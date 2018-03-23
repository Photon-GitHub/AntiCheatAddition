package de.photon.AACAdditionPro.checks.subchecks.clientcontrol;

import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ClientControlModule;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.ViaAPI;

import java.util.List;

public class VersionControl implements Listener, ClientControlModule
{
    @LoadFromConfiguration(configPath = ".1.8")
    private boolean mc18;
    @LoadFromConfiguration(configPath = ".1.9")
    private boolean mc19;
    @LoadFromConfiguration(configPath = ".1.10")
    private boolean mc110;
    @LoadFromConfiguration(configPath = ".1.11")
    private boolean mc111;
    @LoadFromConfiguration(configPath = ".1.12")
    private boolean mc112;

    @LoadFromConfiguration(configPath = ".message")
    private String message;

    private final ViaAPI api = Via.getAPI();

    @EventHandler
    // TODO: ASYNCPLAYERPRELOGINEVENT OR PLAYERLOGINEVENT ?
    public void on(AsyncPlayerPreLoginEvent event)
    {
        boolean allowed = false;
        switch (this.api.getPlayerVersion(event.getUniqueId()))
        {
            case 47:
                allowed = mc18;
                break;
            case 107:
            case 108:
            case 109:
            case 110:
                allowed = mc19;
                break;
            case 210:
                allowed = mc110;
                break;
            case 315:
            case 316:
                allowed = mc111;
                break;
            case 335:
            case 338:
            case 340:
                allowed = mc112;
                break;
        }

        if (!allowed)
        {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, this.message);
        }
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
