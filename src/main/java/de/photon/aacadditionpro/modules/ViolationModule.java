package de.photon.aacadditionpro.modules;

import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import lombok.Getter;

public abstract class ViolationModule extends Module
{
    @Getter(lazy = true) private final ViolationManagement management = createViolationManagement();

    public ViolationModule(String configString)
    {
        super(configString);
    }

    protected abstract ViolationManagement createViolationManagement();
}
