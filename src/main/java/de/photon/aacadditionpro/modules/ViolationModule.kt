package de.photon.aacadditionpro.modules

import com.google.common.collect.ImmutableMap
import de.photon.aacadditionpro.util.violationlevels.ViolationAggregateManagement
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement
import de.photon.aacadditionpro.util.violationlevels.threshold.ThresholdManagement
import java.util.*
import java.util.stream.Collectors

abstract class ViolationModule(configString: String) : Module(configString) {
    val management by lazy { createViolationManagement() }

    fun getAACTooltip(uuid: UUID?, score: Double): Map<String, String> {
        return ImmutableMap.of("Score:", score.toString())
    }

    protected abstract fun createViolationManagement(): ViolationManagement

    companion object {
        fun parentOf(configString: String, vararg children: ViolationModule?): ViolationModule {
            return object : ViolationModule(configString) {
                override fun createViolationManagement(): ViolationManagement {
                    return ViolationAggregateManagement(this,
                                                        ThresholdManagement.loadThresholds(configString),
                                                        Arrays.stream(children).map { management }.collect(Collectors.toSet()))
                }

                override fun createModuleLoader(): ModuleLoader {
                    return ModuleLoader.builder(this).build()
                }
            }
        }
    }
}