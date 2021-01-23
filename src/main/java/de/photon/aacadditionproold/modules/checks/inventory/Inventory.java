package de.photon.aacadditionproold.modules.checks.inventory;

import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionproold.modules.Module;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.modules.ViolationModule;
import de.photon.aacadditionproold.util.violationlevels.ViolationLevelManagement;
import lombok.Getter;

import java.util.Set;

public class Inventory implements ViolationModule
{
    @Getter
    private static final Inventory instance = new Inventory();
    private static final Set<Module> submodules = ImmutableSet.of(AverageHeuristicPattern.getInstance(),
                                                                  HitPattern.getInstance(),
                                                                  MovePattern.getInstance(),
                                                                  MultiInteractionPattern.getInstance(),
                                                                  PerfectExitPattern.getInstance(),
                                                                  RotationPattern.getInstance(),
                                                                  SprintingPattern.getInstance());

    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 80L);

    @Override
    public Set<Module> getSubModules()
    {
        return submodules;
    }

    @Override
    public boolean isSubModule()
    {
        return false;
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.INVENTORY;
    }
}