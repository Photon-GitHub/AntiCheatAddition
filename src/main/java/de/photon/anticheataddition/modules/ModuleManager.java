package de.photon.anticheataddition.modules;

import de.photon.anticheataddition.modules.additions.BrandHider;
import de.photon.anticheataddition.modules.additions.DamageIndicator;
import de.photon.anticheataddition.modules.additions.LogBot;
import de.photon.anticheataddition.modules.additions.esp.Esp;
import de.photon.anticheataddition.modules.additions.informationhider.DurabilityHider;
import de.photon.anticheataddition.modules.additions.informationhider.EnchantmentHider;
import de.photon.anticheataddition.modules.additions.informationhider.ItemCountHider;
import de.photon.anticheataddition.modules.checks.autoeat.AutoEat;
import de.photon.anticheataddition.modules.checks.autofish.AutoFishConsistency;
import de.photon.anticheataddition.modules.checks.autofish.AutoFishInhumanReaction;
import de.photon.anticheataddition.modules.checks.autopotion.AutoPotion;
import de.photon.anticheataddition.modules.checks.duping.DupingDoubleDropped;
import de.photon.anticheataddition.modules.checks.duping.DupingSecretCache;
import de.photon.anticheataddition.modules.checks.fastswitch.Fastswitch;
import de.photon.anticheataddition.modules.checks.autotool.AutoTool;
import de.photon.anticheataddition.modules.checks.impossiblechat.ImpossibleChat;
import de.photon.anticheataddition.modules.checks.inventory.Inventory;
import de.photon.anticheataddition.modules.checks.packetanalysis.*;
import de.photon.anticheataddition.modules.checks.scaffold.Scaffold;
import de.photon.anticheataddition.modules.checks.shield.ShieldHit;
import de.photon.anticheataddition.modules.checks.skinblinker.SkinBlinkerSprinting;
import de.photon.anticheataddition.modules.checks.skinblinker.SkinBlinkerUnusedBit;
import de.photon.anticheataddition.modules.checks.teaming.Teaming;
import de.photon.anticheataddition.modules.checks.tower.Tower;
import de.photon.anticheataddition.modules.sentinel.SentinelChannelModule;
import de.photon.anticheataddition.modules.sentinel.exploits.*;
import de.photon.anticheataddition.modules.sentinel.mods.*;
import de.photon.anticheataddition.util.config.ConfigUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModuleManager
{
    @Getter private static final ModuleMap<Module> moduleMap;
    @Getter private static final ModuleMap<ViolationModule> violationModuleMap;

    static {
        final var moduleList = new ArrayList<>(Arrays.asList(
                // Additions
                BrandHider.INSTANCE,
                DamageIndicator.INSTANCE,

                new Module("InformationHider", DurabilityHider.INSTANCE, EnchantmentHider.INSTANCE, ItemCountHider.INSTANCE),

                Esp.INSTANCE,
                LogBot.INSTANCE,

                // Checks
                AutoEat.INSTANCE,

                ViolationModule.parentOf("AutoFish", AutoFishConsistency.INSTANCE, AutoFishInhumanReaction.INSTANCE),

                AutoPotion.INSTANCE,

                ViolationModule.parentOf("Duping", DupingDoubleDropped.INSTANCE, DupingSecretCache.INSTANCE),

                Fastswitch.INSTANCE,

                AutoTool.INSTANCE,

                ImpossibleChat.INSTANCE,

                Inventory.INSTANCE,

                ViolationModule.parentOf("PacketAnalysis", PacketAnalysisAimStep.INSTANCE, PacketAnalysisAnimation.INSTANCE, PacketAnalysisEqualRotation.INSTANCE, PacketAnalysisIllegalPitch.INSTANCE, PacketAnalysisPerfectRotation.INSTANCE),

                Scaffold.INSTANCE,

                ViolationModule.parentOf("Shield", ShieldHit.INSTANCE),

                ViolationModule.parentOf("Skinblinker", SkinBlinkerUnusedBit.INSTANCE, SkinBlinkerSprinting.INSTANCE),

                Teaming.INSTANCE,

                Tower.INSTANCE,

                // Sentinel
                BookPageSentinel.INSTANCE,
                CommandBlockSentinel.INSTANCE,
                CreativeKillPotionSentinel.INSTANCE,
                SelfDamageSentinel.INSTANCE,
                TrollPotionSentinel.INSTANCE,

                BetterSprintingSentinel.INSTANCE,
                FiveZigSentinel.INSTANCE,
                LabyModSentinel.INSTANCE,
                SchematicaSentinel.INSTANCE,
                VapeSentinel.INSTANCE,
                WorldDownloaderSentinel.INSTANCE));

        // Add sentinel custom modules.
        ConfigUtil.loadKeys("Sentinel.Custom").stream().map(key -> new SentinelChannelModule("Custom." + key)).forEach(moduleList::add);

        moduleMap = new ModuleMap<>(moduleList);
        violationModuleMap = new ModuleMap<>(moduleMap.values().stream()
                                                      .filter(ViolationModule.class::isInstance)
                                                      .map(ViolationModule.class::cast)
                                                      .toList());

        // Use moduleList to make sure the initial enabling log is sorted.
        for (Module module : moduleList) module.activate();
    }

    public static void addExternalModule(final Module externalModule)
    {
        moduleMap.addModule(externalModule);
        if (externalModule instanceof ViolationModule vlModule) violationModuleMap.addModule(vlModule);
    }
}
