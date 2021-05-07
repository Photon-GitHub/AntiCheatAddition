package de.photon.aacadditionpro.util.visibility

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.events.PacketListener
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import de.photon.aacadditionpro.AACAdditionPro
import de.photon.aacadditionpro.ServerVersion
import de.photon.aacadditionpro.user.data.Constants
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.ChunkUnloadEvent
import java.util.*

internal abstract class PlayerInformationHider protected constructor(vararg affectedPackets: PacketType) : Listener {
    private val informationPacketListener: PacketListener
    private val hiddenFromPlayerMap: Multimap<Int, Int>

    fun clear() {
        synchronized(hiddenFromPlayerMap) {
            hiddenFromPlayerMap.clear()
        }
    }

    fun registerListeners() {
        // Only start if the ServerVersion is supported
        if (ServerVersion.supportsActiveServerVersion(supportedVersions)) {
            // Register events and packet listener
            AACAdditionPro.getInstance().registerListener(this)
            ProtocolLibrary.getProtocolManager().addPacketListener(informationPacketListener)
        }
    }

    fun unregisterListeners() {
        HandlerList.unregisterAll(this)
        ProtocolLibrary.getProtocolManager().removePacketListener(informationPacketListener)
    }

    protected open val supportedVersions: Set<ServerVersion> get() = EnumSet.allOf(ServerVersion::class.java)

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        removeEntity(event.entity)
    }

    @EventHandler
    fun onChunkUnload(event: ChunkUnloadEvent) {
        // Cache entities for performance reasons so the server doesn't need to load them again when the
        // task is executed.
        synchronized(hiddenFromPlayerMap) {
            for (entity in event.chunk.entities) removeEntity(entity)
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        removeEntity(event.player)
    }

    /**
     * Remove the given entity from the underlying map.
     *
     * @param entity - the entity to remove.
     */
    @Suppress("ControlFlowWithEmptyBody")
    private fun removeEntity(entity: Entity) {
        synchronized(hiddenFromPlayerMap) {
            hiddenFromPlayerMap.removeAll(entity.entityId)
            // Remove all the instances of entity from the values.
            while (hiddenFromPlayerMap.values().remove(entity.entityId));
        }
    }

    /**
     * Hides a [Player] from another [Player].
     */
    fun hidePlayer(observer: Player?, playerToHide: Player?) {
        checkNotNull(observer) { "Tried to hide information from null observer." }
        checkNotNull(playerToHide) { "Tried to hide information of null player." }

        synchronized(hiddenFromPlayerMap) {
            hiddenFromPlayerMap.put(observer.entityId, playerToHide.entityId)
        }

        onHide(observer, playerToHide)
    }

    protected abstract fun onHide(observer: Player, playerToHide: Player)

    /**
     * Unhides a [Player] from another [Player].
     */
    fun revealPlayer(observer: Player?, playerToReveal: Player?) {
        checkNotNull(observer) { "Tried to reveal information to null observer." }
        checkNotNull(playerToReveal) { "Tried to reveal information of null player." }

        var hiddenBefore: Boolean
        synchronized(hiddenFromPlayerMap) {
            hiddenBefore = hiddenFromPlayerMap.remove(observer.entityId, playerToReveal.entityId)
        }

        // Resend packets
        if (ProtocolLibrary.getProtocolManager() != null && hiddenBefore) {
            Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), Runnable { ProtocolLibrary.getProtocolManager().updateEntity(playerToReveal, ImmutableList.of(observer)) })
        }
    }

    init {
        hiddenFromPlayerMap = MultimapBuilder.hashKeys(Constants.SERVER_EXPECTED_PLAYERS).hashSetValues(Constants.WORLD_EXPECTED_PLAYERS).build()

        informationPacketListener = object : PacketAdapter(AACAdditionPro.getInstance(), ListenerPriority.NORMAL, ImmutableSet.copyOf(affectedPackets)) {
            override fun onPacketSending(event: PacketEvent) {
                val entityID = event.packet.integers.read(0)

                if (!event.isPlayerTemporary) {
                    val hidden: Boolean
                    synchronized(hiddenFromPlayerMap) {
                        hidden = hiddenFromPlayerMap.containsEntry(event.player.entityId, entityID)
                    }
                    if (hidden) event.isCancelled = true
                }
            }
        }
    }
}