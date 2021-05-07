package de.photon.aacadditionpro.events

import de.photon.aacadditionpro.ServerVersion
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList

open class SentinelEvent : ModulePlayerEvent {

    protected constructor(p: Player?, moduleId: String?) : super(p!!, moduleId!!)
    protected constructor(player: Player?, moduleId: String?, legacy: Boolean) : super(player!!, moduleId!!, legacy)

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        //Needed for 1.8.8
        @JvmStatic
        val handlerList = HandlerList()

        @JvmStatic
        fun build(player: Player?, moduleId: String?): SentinelEvent {
            return if (ServerVersion.supportsActiveServerVersion(ServerVersion.LEGACY_EVENT_VERSIONS)) SentinelEvent(player, moduleId, true)
            else SentinelEvent(player, moduleId)
        }
    }
}