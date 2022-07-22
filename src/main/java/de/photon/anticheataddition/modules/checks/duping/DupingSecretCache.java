package de.photon.anticheataddition.modules.checks.duping;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

public final class DupingSecretCache extends ViolationModule
{
    public static final DupingSecretCache INSTANCE = new DupingSecretCache();

    protected DupingSecretCache()
    {
        super("Duping.parts.SecretCache");
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).loadThresholdsToManagement().withDecay(18000L, 20).build();
    }
}
