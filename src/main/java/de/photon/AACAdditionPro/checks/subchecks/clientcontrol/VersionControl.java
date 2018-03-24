package de.photon.AACAdditionPro.checks.subchecks.clientcontrol;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ClientControlModule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.ViaAPI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VersionControl implements Listener, ClientControlModule
{
    private final boolean mc18;
    private final boolean mc19;
    private final boolean mc110;
    private final boolean mc111;
    private final boolean mc112;

    private final String message;

    private final ViaAPI api = Via.getAPI();

    public VersionControl()
    {
        mc18 = AACAdditionPro.getInstance().getConfig().getBoolean("ClientControl.VersionControl.1.8");
        mc19 = AACAdditionPro.getInstance().getConfig().getBoolean("ClientControl.VersionControl.1.9");
        mc110 = AACAdditionPro.getInstance().getConfig().getBoolean("ClientControl.VersionControl.1.10");
        mc111 = AACAdditionPro.getInstance().getConfig().getBoolean("ClientControl.VersionControl.1.11");
        mc112 = AACAdditionPro.getInstance().getConfig().getBoolean("ClientControl.VersionControl.1.12");

        // Message
        Collection<String> versionStrings = new ArrayList<>();
        if (mc18)
        {
            versionStrings.add("1.8");
        }

        if (mc19)
        {
            versionStrings.add("1.9");
        }

        if (mc110)
        {
            versionStrings.add("1.10");
        }

        if (mc111)
        {
            versionStrings.add("1.11");
        }

        if (mc112)
        {
            versionStrings.add("1.12");
        }

        // Get the message
        this.message = AACAdditionPro.getInstance().getConfig().getString("ClientControl.VersionControl.message")
                                     // Replace the special placeholder
                                     .replace("{supportedVersions}", String.join(", ", versionStrings));
    }

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
    public Set<String> getDependencies()
    {
        return new HashSet<>(Collections.singletonList("ViaVersion"));
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.VERSION_CONTROL;
    }
}
