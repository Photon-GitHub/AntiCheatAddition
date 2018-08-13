package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;
import org.bukkit.event.Listener;

public class Fastthrow implements Listener, ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 120L);



    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.FASTTHROW;
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }
}