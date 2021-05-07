package de.photon.aacadditionpro.events

import de.photon.aacadditionpro.ServerVersion
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList

open class ViolationEvent : ModulePlayerEvent {
    val vl: Int

    protected constructor(p: Player?, moduleId: String?, vl: Int) : super(p!!, moduleId!!) {
        this.vl = vl
    }

    protected constructor(player: Player?, moduleId: String?, vl: Int, legacy: Boolean) : super(player!!, moduleId!!, legacy) {
        this.vl = vl
    }

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        //Needed for 1.8.8
        @JvmStatic
        val handlerList = HandlerList()

        @JvmStatic
        fun build(player: Player?, moduleId: String?, vl: Int): ViolationEvent {
            return if (ServerVersion.supportsActiveServerVersion(ServerVersion.LEGACY_EVENT_VERSIONS)) ViolationEvent(player, moduleId, vl, true)
            else ViolationEvent(player, moduleId, vl)
        }
    }
}