package de.photon.aacadditionpro.events

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import java.util.function.Consumer

abstract class ModulePlayerEvent : Event, Cancellable {
    val player: Player
    val moduleId: String
    private var cancelled = false


    /**
     * Constructor for 1.14 and onwards.
     */
    protected constructor(p: Player, moduleId: String) : super(!Bukkit.isPrimaryThread()) {
        player = p
        this.moduleId = moduleId
    }

    /**
     * Dummy constructor for legacy minecraft versions before 1.14.
     */
    protected constructor(player: Player, moduleId: String, legacy: Boolean) : super() {
        this.player = player
        this.moduleId = moduleId
    }

    fun call(): ModulePlayerEvent {
        Bukkit.getPluginManager().callEvent(this)
        return this
    }

    fun runIfUncancelled(consumer: Consumer<ModulePlayerEvent?>) {
        if (!this.isCancelled) consumer.accept(this)
    }

    override fun setCancelled(b: Boolean) {
        cancelled = b
    }

    override fun isCancelled(): Boolean {
        return cancelled
    }
}