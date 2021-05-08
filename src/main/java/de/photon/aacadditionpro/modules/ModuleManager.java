package de.photon.aacadditionpro.modules;

import de.photon.aacadditionpro.modules.checks.AutoEat;
import de.photon.aacadditionpro.modules.checks.autofish.AutoFishConsistency;
import lombok.Getter;
import lombok.val;
import me.konsolas.aac.api.AACCustomFeature;
import me.konsolas.aac.api.AACCustomFeatureProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

    /**
     * This creates the actual hook for the AAC API.
     */
    public static AACCustomFeatureProvider getCustomFeatureProvider()
    {
        return offlinePlayer -> {
            final List<AACCustomFeature> featureList = new ArrayList<>(violationModuleMap.size());
            final UUID uuid = offlinePlayer.getUniqueId();
            double score;
            for (ViolationModule module : violationModuleMap.values()) {
                // Only add enabled modules
                if (module.isLoaded()) {
                    score = module.getManagement().getVL(uuid);
                    featureList.add(new AACCustomFeature(module.getConfigString(), module.getAacInfo(), score, module.getAACTooltip(uuid, score)));
                }
            }
            return featureList;
        };
    }
}
