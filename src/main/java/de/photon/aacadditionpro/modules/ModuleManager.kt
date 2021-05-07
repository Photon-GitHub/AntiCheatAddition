package de.photon.aacadditionpro.modules

import de.photon.aacadditionpro.modules.checks.AutoEat
import de.photon.aacadditionpro.modules.checks.autofish.AutoFishConsistency
import me.konsolas.aac.api.AACCustomFeature
import me.konsolas.aac.api.AACCustomFeatureProvider
import org.bukkit.OfflinePlayer

object ModuleManager {
    lateinit var moduleMap: ModuleMap<Module>
    lateinit var violationModuleMap: ModuleMap<ViolationModule>

    fun enable() {
        val autoFishConsistency = AutoFishConsistency()
        val autoFish = ViolationModule.parentOf("AutoFish", autoFishConsistency)

        val autoEat = AutoEat()

        moduleMap = ModuleMap(autoFish, autoFishConsistency,
                              autoEat)
        violationModuleMap = ModuleMap<ViolationModule>(moduleMap.values().filterIsInstance<ViolationModule>())
    }

    fun addExternalModule(module: Module?) {
        checkNotNull(module) { "Tried to add null module." }
        this.moduleMap.addModule(module)
    }

    fun addExternalModule(module: ViolationModule?) {
        checkNotNull(module) { "Tried to add null module." }
        this.moduleMap.addModule(module)
        this.violationModuleMap.addModule(module)
    }

    /**
     * This creates the actual hook for the AAC API.
     */
    fun getCustomFeatureProvider(): AACCustomFeatureProvider {
        return AACCustomFeatureProvider { offlinePlayer: OfflinePlayer ->
            val uuid = offlinePlayer.uniqueId
            val featureList = mutableListOf<AACCustomFeature>()
            for (vm in violationModuleMap.values()) {
                // Only add enabled modules
                if (vm.isLoaded) {
                    val score = vm.management.getVL(uuid).toDouble()
                    featureList.add(AACCustomFeature(vm.configString, vm.aacInfo, score, vm.getAACTooltip(uuid, score)))
                }
            }
            return@AACCustomFeatureProvider featureList
        }
    }
}