package de.photon.aacadditionpro.modules;

import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;

public class ViolationModule extends Module
{
    private final ViolationManagement management;

    public ViolationModule(String configString, ModuleLoader moduleLoader, ViolationManagement management)
    {
        super(configString, moduleLoader);
        this.management = management;
    }
}
