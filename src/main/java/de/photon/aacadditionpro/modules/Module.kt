package de.photon.aacadditionpro.modules

import de.photon.aacadditionpro.AACAdditionPro
import de.photon.aacadditionpro.InternalPermission
import java.util.*

abstract class Module(val configString: String) {
    val moduleId: String = generateModuleId(configString)
    @JvmField protected val bypassPermission: String = InternalPermission.bypassPermissionOf(moduleId)
    val moduleLoader by lazy { createModuleLoader() }
    val aacInfo: String = AACAdditionPro.getInstance().config.getString(configString + "aac_status_message", configString)!!
    var isLoaded = false
        private set

    fun setEnabled(enabled: Boolean) {
        if (isLoaded != enabled) {
            if (enabled) {
                enableModule()
            } else {
                disableModule()
            }
        }
    }

    fun enableModule() {
        isLoaded = true
        moduleLoader.load()
        enable()
    }

    fun disableModule() {
        isLoaded = false
        moduleLoader.unload()
        disable()
    }

    protected abstract fun createModuleLoader(): ModuleLoader

    protected fun enable() {}
    protected fun disable() {}

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Module) return false
        return moduleId == other.moduleId
    }

    override fun hashCode(): Int {
        return moduleId.hashCode()
    }

    override fun toString(): String {
        return "Module(configString=$configString, moduleId=$moduleId, bypassPermission=$bypassPermission, moduleLoader=$moduleLoader, aacInfo=$aacInfo, loaded=$isLoaded)"
    }

    companion object {
        @JvmStatic
        fun generateModuleId(configString: String): String {
            return "aacadditionpro_" + configString.lowercase(Locale.ENGLISH)
        }
    }
}