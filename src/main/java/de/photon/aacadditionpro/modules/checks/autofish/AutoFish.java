package de.photon.aacadditionpro.modules.checks.autofish;

import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.RestrictedServerVersion;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import lombok.Getter;

import java.util.Set;

public class AutoFish implements ViolationModule, RestrictedServerVersion
{
    @Getter
    private static final AutoFish instance = new AutoFish();

    private static final Set<Module> submodules = ImmutableSet.of(ConsistencyPattern.getInstance(), InhumanReactionPattern.getInstance());

    @Getter
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 600);

    @Getter
    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancelVl;


    @Override
    public boolean isSubModule()
    {
        return false;
    }

    @Override
    public Set<Module> getSubModules()
    {
        return submodules;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.AUTO_FISH;
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public Set<ServerVersion> getSupportedVersions()
    {
        return ServerVersion.NON_188_VERSIONS;
    }
}
