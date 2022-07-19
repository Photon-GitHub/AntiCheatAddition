package de.photon.anticheataddition.modules;

import de.photon.anticheataddition.modules.additions.BrandHider;
import de.photon.anticheataddition.modules.additions.DamageIndicator;
import de.photon.anticheataddition.modules.additions.EnchantmentHider;
import de.photon.anticheataddition.modules.additions.LogBot;
import de.photon.anticheataddition.modules.additions.esp.Esp;
import de.photon.anticheataddition.modules.checks.autoeat.AutoEat;
import de.photon.anticheataddition.modules.checks.autofish.AutoFishConsistency;
import de.photon.anticheataddition.modules.checks.autofish.AutoFishInhumanReaction;
import de.photon.anticheataddition.modules.checks.autopotion.AutoPotion;
import de.photon.anticheataddition.modules.checks.fastswitch.Fastswitch;
import de.photon.anticheataddition.modules.checks.impossiblechat.ImpossibleChat;
import de.photon.anticheataddition.modules.checks.inventory.InventoryAverageHeuristic;
import de.photon.anticheataddition.modules.checks.inventory.InventoryHit;
import de.photon.anticheataddition.modules.checks.inventory.InventoryMove;
import de.photon.anticheataddition.modules.checks.inventory.InventoryMultiInteraction;
import de.photon.anticheataddition.modules.checks.inventory.InventoryPerfectExit;
import de.photon.anticheataddition.modules.checks.inventory.InventoryRotation;
import de.photon.anticheataddition.modules.checks.inventory.InventorySprinting;
import de.photon.anticheataddition.modules.checks.packetanalysis.PacketAnalysisAnimation;
import de.photon.anticheataddition.modules.checks.packetanalysis.PacketAnalysisEqualRotation;
import de.photon.anticheataddition.modules.checks.packetanalysis.PacketAnalysisIllegalPitch;
import de.photon.anticheataddition.modules.checks.pingspoof.Pingspoof;
import de.photon.anticheataddition.modules.checks.scaffold.Scaffold;
import de.photon.anticheataddition.modules.checks.skinblinker.SkinBlinkerSprinting;
import de.photon.anticheataddition.modules.checks.skinblinker.SkinBlinkerUnusedBit;
import de.photon.anticheataddition.modules.checks.teaming.Teaming;
import de.photon.anticheataddition.modules.checks.tower.Tower;
import de.photon.anticheataddition.modules.sentinel.SentinelChannelModule;
import de.photon.anticheataddition.modules.sentinel.exploits.BookPageSentinel;
import de.photon.anticheataddition.modules.sentinel.exploits.CommandBlockSentinel;
import de.photon.anticheataddition.modules.sentinel.exploits.CreativeKillPotionSentinel;
import de.photon.anticheataddition.modules.sentinel.exploits.SelfDamageSentinel;
import de.photon.anticheataddition.modules.sentinel.exploits.TrollPotionSentinel;
import de.photon.anticheataddition.modules.sentinel.mods.BetterSprintingSentinel;
import de.photon.anticheataddition.modules.sentinel.mods.FiveZigSentinel;
import de.photon.anticheataddition.modules.sentinel.mods.LabyModSentinel;
import de.photon.anticheataddition.modules.sentinel.mods.SchematicaSentinel;
import de.photon.anticheataddition.modules.sentinel.mods.VapeSentinel;
import de.photon.anticheataddition.modules.sentinel.mods.WorldDownloaderSentinel;
import de.photon.anticheataddition.util.config.ConfigUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.val;

import java.util.ArrayList;
import java.util.Arrays;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModuleManager
{
    @Getter private static final ModuleMap<Module> moduleMap;
    @Getter private static final ModuleMap<ViolationModule> violationModuleMap;

    static {
        val moduleList = new ArrayList<>(Arrays.asList(
                // Additions
                BrandHider.INSTANCE,
                DamageIndicator.INSTANCE,
                EnchantmentHider.INSTANCE,
                Esp.INSTANCE,
                LogBot.INSTANCE,

                // Checks
                AutoEat.INSTANCE,

                AutoFishConsistency.INSTANCE,
                AutoFishInhumanReaction.INSTANCE,
                ViolationModule.parentOf("AutoFish", AutoFishConsistency.INSTANCE, AutoFishInhumanReaction.INSTANCE),

                AutoPotion.INSTANCE,

                Fastswitch.INSTANCE,

                ImpossibleChat.INSTANCE,

                InventoryAverageHeuristic.INSTANCE,
                InventoryHit.INSTANCE,
                InventoryMove.INSTANCE,
                InventoryMultiInteraction.INSTANCE,
                InventoryPerfectExit.INSTANCE,
                InventoryRotation.INSTANCE,
                InventorySprinting.INSTANCE,
                ViolationModule.parentOf("Inventory", InventoryAverageHeuristic.INSTANCE, InventoryHit.INSTANCE, InventoryMove.INSTANCE, InventoryMultiInteraction.INSTANCE, InventoryPerfectExit.INSTANCE, InventoryRotation.INSTANCE, InventorySprinting.INSTANCE),

                PacketAnalysisAnimation.INSTANCE,
                PacketAnalysisEqualRotation.INSTANCE,
                PacketAnalysisIllegalPitch.INSTANCE,
                ViolationModule.parentOf("PacketAnalysis", PacketAnalysisAnimation.INSTANCE, PacketAnalysisEqualRotation.INSTANCE, PacketAnalysisIllegalPitch.INSTANCE),

                Pingspoof.INSTANCE,

                Scaffold.INSTANCE.getScaffoldAngle(),
                Scaffold.INSTANCE.getScaffoldJumping(),
                Scaffold.INSTANCE.getScaffoldPosition(),
                Scaffold.INSTANCE.getScaffoldRotationDerivative(),
                Scaffold.INSTANCE.getScaffoldRotationFastChange(),
                Scaffold.INSTANCE.getScaffoldRotationSecondDerivative(),
                Scaffold.INSTANCE.getScaffoldSafewalkPosition(),
                Scaffold.INSTANCE.getScaffoldSafewalkTiming(),
                Scaffold.INSTANCE.getScaffoldSprinting(),
                Scaffold.INSTANCE,

                SkinBlinkerUnusedBit.INSTANCE,
                SkinBlinkerSprinting.INSTANCE,
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
        for (Module module : moduleList) module.enableModule();
    }

    public static void addExternalModule(final Module externalModule)
    {
        moduleMap.addModule(externalModule);
        if (externalModule instanceof ViolationModule vlModule) violationModuleMap.addModule(vlModule);
    }
}
