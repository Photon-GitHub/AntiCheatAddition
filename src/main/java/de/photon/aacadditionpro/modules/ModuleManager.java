package de.photon.aacadditionpro.modules;

import de.photon.aacadditionpro.modules.additions.BrandHider;
import de.photon.aacadditionpro.modules.additions.DamageIndicator;
import de.photon.aacadditionpro.modules.checks.AutoEat;
import de.photon.aacadditionpro.modules.checks.autofish.AutoFishConsistency;
import de.photon.aacadditionpro.modules.sentinel.BetterSprintingSentinel;
import de.photon.aacadditionpro.modules.sentinel.FiveZigSentinel;
import de.photon.aacadditionpro.modules.sentinel.LabyModSentinel;
import de.photon.aacadditionpro.modules.sentinel.SentinelChannelModule;
import de.photon.aacadditionpro.util.config.ConfigUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.val;
import me.konsolas.aac.api.AACCustomFeature;
import me.konsolas.aac.api.AACCustomFeatureProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModuleManager
{
    @Getter private static final ModuleMap<Module> moduleMap;
    @Getter private static final ModuleMap<ViolationModule> violationModuleMap;

    static {
        // Additions
        val brandHider = new BrandHider();
        val damageIndicator = new DamageIndicator();

        // Checks
        val autoEat = new AutoEat();

        val autoFishConsistency = new AutoFishConsistency();
        val autoFish = ViolationModule.parentOf("AutoFish", autoFishConsistency);

        // Sentinel
        val betterSprintingSentinel = new BetterSprintingSentinel();
        val fiveZigSentinel = new FiveZigSentinel();
        val labyModSentinel = new LabyModSentinel();

        val moduleList = new ArrayList<>(Arrays.asList(
                // Additions
                brandHider,
                damageIndicator,

                // Checks
                autoEat,
                autoFish, autoFishConsistency,

                //Sentinel
                betterSprintingSentinel,
                fiveZigSentinel,
                labyModSentinel));

        // Add sentinel custom modules.
        ConfigUtils.loadKeys("Sentinel.Custom").stream().map(key -> new SentinelChannelModule("Sentinel.Custom." + key)).forEach(moduleList::add);

        moduleMap = new ModuleMap<>(moduleList);
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
            val featureList = new ArrayList<AACCustomFeature>(violationModuleMap.size());
            val uuid = offlinePlayer.getUniqueId();
            double score;
            for (ViolationModule module : violationModuleMap.values()) {
                // Only add enabled modules
                if (module.isEnabled()) {
                    score = module.getManagement().getVL(uuid);
                    featureList.add(new AACCustomFeature(module.getConfigString(), module.getAacInfo(), score, module.getAACTooltip(uuid, score)));
                }
            }
            return featureList;
        };
    }
}
