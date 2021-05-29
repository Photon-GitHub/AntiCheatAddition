package de.photon.aacadditionpro.modules.checks.pingspoof;

import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;

public class Pingspoof extends ViolationModule
{
    public Pingspoof()
    {
        super("Pingspoof");
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return null;
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return null;
    }
}
