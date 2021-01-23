package de.photon.aacadditionproold.modules.checks.autofish;

import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionproold.ServerVersion;
import de.photon.aacadditionproold.modules.Module;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.modules.RestrictedServerVersion;
import de.photon.aacadditionproold.modules.ViolationModule;
import de.photon.aacadditionproold.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionproold.util.violationlevels.ViolationLevelManagement;
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
