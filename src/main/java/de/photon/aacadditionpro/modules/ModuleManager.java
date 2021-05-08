package de.photon.aacadditionpro.modules;

import de.photon.aacadditionpro.modules.checks.AutoEat;
import de.photon.aacadditionpro.modules.checks.autofish.AutoFishConsistency;
import lombok.Getter;
import lombok.val;

import java.util.stream.Collectors;

public class ModuleManager
{
    @Getter private static final ModuleMap<Module> moduleMap;
    @Getter private static final ModuleMap<ViolationModule> violationModuleMap;

    static {
        val autoEat = new AutoEat();

        val autoFishConsistency = new AutoFishConsistency();
        val autoFish = ViolationModule.parentOf("AutoFish", autoFishConsistency);

        moduleMap = new ModuleMap<>(autoEat,
                                    autoFish, autoFishConsistency);

        violationModuleMap = new ModuleMap<>(moduleMap.values().stream()
                                                      .filter(ViolationModule.class::isInstance)
                                                      .map(ViolationModule.class::cast)
                                                      .collect(Collectors.toList()));
    }

    private static void addExternalModule(final Module externalModule)
    {
        moduleMap.addModule(externalModule);
        if (externalModule instanceof ViolationModule) violationModuleMap.addModule((ViolationModule) externalModule);
    }
}
