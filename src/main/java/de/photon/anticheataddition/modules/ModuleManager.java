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
import de.photon.anticheataddition.modules.sentinel.BetterSprintingSentinel;
import de.photon.anticheataddition.modules.sentinel.FiveZigSentinel;
import de.photon.anticheataddition.modules.sentinel.LabyModSentinel;
import de.photon.anticheataddition.modules.sentinel.SchematicaSentinel;
import de.photon.anticheataddition.modules.sentinel.SentinelChannelModule;
import de.photon.anticheataddition.modules.sentinel.VapeSentinel;
import de.photon.anticheataddition.modules.sentinel.WorldDownloaderSentinel;
import de.photon.anticheataddition.modules.sentinel.exploits.CommandBlockSentinel;
import de.photon.anticheataddition.modules.sentinel.exploits.CreativeKillPotionSentinel;
import de.photon.anticheataddition.modules.sentinel.exploits.SelfDamageSentinel;
import de.photon.anticheataddition.modules.sentinel.exploits.TrollPotionSentinel;
import de.photon.anticheataddition.util.config.ConfigUtils;
import de.photon.anticheataddition.util.datastructure.Pair;
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
        val brandHider = BrandHider.INSTANCE;
        val damageIndicator = new DamageIndicator();
        val enchantmentHider = new EnchantmentHider();
        val esp = new Esp();
        val logBot = new LogBot();

        // Checks
        val autoEat = new AutoEat();

        val autoFishConsistency = new AutoFishConsistency();
        val autoFishInhumanReaction = new AutoFishInhumanReaction();
        val autoFish = ViolationModule.parentOf("AutoFish", autoFishConsistency, autoFishInhumanReaction);

        val autoPotion = new AutoPotion();

        val fastswitch = new Fastswitch();

        val impossibleChat = new ImpossibleChat();

        val inventoryAverageHeuristic = new InventoryAverageHeuristic();
        val inventoryHit = new InventoryHit();
        val inventoryMove = new InventoryMove();
        val inventoryMultiInteraction = new InventoryMultiInteraction();
        val inventoryPerfectExit = new InventoryPerfectExit();
        val inventoryRotation = new InventoryRotation();
        val inventorySprinting = new InventorySprinting();
        val inventory = ViolationModule.parentOf("Inventory", inventoryAverageHeuristic, inventoryHit, inventoryMove, inventoryMultiInteraction, inventoryPerfectExit, inventoryRotation, inventorySprinting);


        val packetAnalysisAnimation = new PacketAnalysisAnimation();
        val packetAnalysisEqualRotation = new PacketAnalysisEqualRotation();
        val packetAnalysisIllegalPitch = new PacketAnalysisIllegalPitch();
        val packetAnalysis = ViolationModule.parentOf("PacketAnalysis", packetAnalysisAnimation, packetAnalysisEqualRotation, packetAnalysisIllegalPitch);

        val pingspoof = new Pingspoof();

        val scaffold = new Scaffold();

        val skinBlinkerUnusedBit = new SkinBlinkerUnusedBit();
        val skinBlinkerSprinting = new SkinBlinkerSprinting();
        val skinBlinker = ViolationModule.parentOf("Skinblinker", skinBlinkerUnusedBit, skinBlinkerSprinting);

        val teaming = new Teaming();

        val tower = new Tower();

        // Sentinel
        val commandBlockSentinel = new CommandBlockSentinel();
        val creativeKillPotionSentinel = new CreativeKillPotionSentinel();
        val selfDamageSentinel = new SelfDamageSentinel();
        val trollPotionSentinel = new TrollPotionSentinel();

        val betterSprintingSentinel = new BetterSprintingSentinel();
        val fiveZigSentinel = new FiveZigSentinel();
        val labyModSentinel = new LabyModSentinel();
        val schematicaSentinel = new SchematicaSentinel();
        val vapeSentinel = new VapeSentinel();
        val worldDownloaderSentinel = new WorldDownloaderSentinel();

        val moduleList = new ArrayList<>(Arrays.asList(
                // Additions
                brandHider,
                damageIndicator,
                enchantmentHider,
                esp,
                logBot,

                // Checks
                autoEat,

                autoFishConsistency,
                autoFishInhumanReaction,
                autoFish,

                autoPotion,

                fastswitch,

                impossibleChat,

                inventoryAverageHeuristic,
                inventoryHit,
                inventoryMove,
                inventoryMultiInteraction,
                inventoryPerfectExit,
                inventoryRotation,
                inventorySprinting,
                inventory,

                packetAnalysisAnimation,
                packetAnalysisEqualRotation,
                packetAnalysisIllegalPitch,
                packetAnalysis,

                pingspoof,

                scaffold.getScaffoldAngle(),
                scaffold.getScaffoldJumping(),
                scaffold.getScaffoldPosition(),
                scaffold.getScaffoldRotationDerivative(),
                scaffold.getScaffoldRotationFastChange(),
                scaffold.getScaffoldRotationSecondDerivative(),
                scaffold.getScaffoldSafewalkPosition(),
                scaffold.getScaffoldSafewalkTiming(),
                scaffold.getScaffoldSprinting(),
                scaffold,

                skinBlinkerUnusedBit,
                skinBlinkerSprinting,
                skinBlinker,

                teaming,

                tower,

                // Sentinel
                commandBlockSentinel,
                creativeKillPotionSentinel,
                selfDamageSentinel,
                trollPotionSentinel,

                betterSprintingSentinel,
                fiveZigSentinel,
                labyModSentinel,
                schematicaSentinel,
                vapeSentinel,
                worldDownloaderSentinel));

        // Add sentinel custom modules.
        ConfigUtils.loadKeys("Sentinel.Custom").stream().map(key -> new SentinelChannelModule("Custom." + key)).forEach(moduleList::add);

        moduleMap = new ModuleMap<>(moduleList);
        violationModuleMap = new ModuleMap<>(moduleMap.values().stream()
                                                      .filter(ViolationModule.class::isInstance)
                                                      .map(ViolationModule.class::cast)
                                                      .collect(Collectors.toList()));

        // Use moduleList to make sure the initial enabling log is sorted.
        for (Module module : moduleList) module.enableModule();
    }

    public static void addExternalModule(final Module externalModule)
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
            val uuid = offlinePlayer.getUniqueId();
            return violationModuleMap.values().stream()
                                     .filter(Module::isEnabled)
                                     .filter(module -> module.getAacInfo() != null)
                                     // Map the module and its AACScore to an AACCustomFeature.
                                     .map(module -> Pair.map(module, module.getAACScore(uuid),
                                                             (m, score) -> new AACCustomFeature(m.getConfigString(), m.getAacInfo(), score, m.getAACTooltip(uuid, score))))
                                     .collect(Collectors.toList());
        };
    }
}
