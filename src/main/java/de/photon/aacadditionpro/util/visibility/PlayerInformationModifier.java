package de.photon.aacadditionpro.util.visibility;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public abstract class PlayerInformationModifier implements Listener
{
    private final Table<Integer, Integer, Boolean> observerEntityMap = HashBasedTable.create();

    private PacketListener informationPacketListener = new PacketAdapter(AACAdditionPro.getInstance(), ListenerPriority.NORMAL, this.getAffectedPackets())
    {
        @Override
        public void onPacketSending(final PacketEvent event)
        {
            final int entityID = event.getPacket().getIntegers().read(0);

            // Make sure that we do not have temporary players who cause problems in the mapping.
            if (!event.isPlayerTemporary() &&
                // See if this packet should be cancelled
                isInformationModified(event.getPlayer(), entityID))
            {
                event.setCancelled(true);
            }
        }
    };

    // For validating the input parameters
    protected static void validate(final Player observer, final Entity entity)
    {
        Preconditions.checkNotNull(observer, "observer cannot be NULL.");
        Preconditions.checkNotNull(entity, "entity cannot be NULL.");
    }

    public void registerListeners()
    {
        // Only start if the ServerVersion is supported
        if (ServerVersion.supportsActiveServerVersion(this.getSupportedVersions())) {
            // Register events and packet listener
            AACAdditionPro.getInstance().registerListener(this);
            ProtocolLibrary.getProtocolManager().addPacketListener(this.informationPacketListener);
        }
    }

    public void unregisterListeners()
    {
        HandlerList.unregisterAll(this);
        ProtocolLibrary.getProtocolManager().removePacketListener(this.informationPacketListener);
    }

    public void resetTable()
    {
        synchronized (observerEntityMap) {
            this.observerEntityMap.clear();
        }
    }

    protected abstract PacketType[] getAffectedPackets();

    protected Set<ServerVersion> getSupportedVersions()
    {
        return EnumSet.allOf(ServerVersion.class);
    }

    @EventHandler
    public void onEntityDeath(final EntityDeathEvent event)
    {
        removeEntity(event.getEntity());
    }

    @EventHandler
    public void onChunkUnload(final ChunkUnloadEvent event)
    {
        // Cache entities for performance reasons so the server doesn't need to load them again when the
        // task is executed.
        for (final Entity entity : event.getChunk().getEntities()) {
            removeEntity(entity);
        }
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event)
    {
        removePlayer(event.getPlayer());
    }

    /**
     * Determine if the given entity and observer is present in the table.
     *
     * @param observer - the player observer.
     * @param entityID - ID of the entity.
     *
     * @return TRUE if they are present, FALSE otherwise.
     */
    private boolean getMembership(final Player observer, final int entityID)
    {
        synchronized (observerEntityMap) {
            return observerEntityMap.contains(observer.getEntityId(), entityID);
        }
    }

    /**
     * Add or remove the given entity and observer entry from the table.
     *
     * @param observer - the player observer.
     * @param entityID - ID of the entity.
     * @param member   - TRUE if they should be present in the table, FALSE otherwise.
     *
     * @return TRUE if they already were present, FALSE otherwise.
     */
    // Helper method
    private boolean setMembership(final Player observer, final int entityID, final boolean member)
    {
        synchronized (observerEntityMap) {
            return member ?
                   observerEntityMap.put(observer.getEntityId(), entityID, true) != null :
                   observerEntityMap.remove(observer.getEntityId(), entityID) != null;
        }
    }

    /**
     * Remove the given entity from the underlying map.
     *
     * @param entity - the entity to remove.
     */
    private void removeEntity(final Entity entity)
    {
        synchronized (observerEntityMap) {
            final int entityID = entity.getEntityId();

            for (final Map<Integer, Boolean> maps : observerEntityMap.rowMap().values()) {
                maps.remove(entityID);
            }
        }
    }

    /**
     * Invoked when a player logs out.
     *
     * @param player - the player that just logged out.
     */
    private void removePlayer(final Player player)
    {
        synchronized (observerEntityMap) {
            // Cleanup
            observerEntityMap.rowMap().remove(player.getEntityId());
        }
    }

    /**
     * Allow the observer to see an entity that was previously hidden.
     *
     * @param observer - the observer.
     * @param entity   - the entity to show.
     */
    public final void unModifyInformation(final Player observer, final Entity entity)
    {
        validate(observer, entity);
        final boolean hiddenBefore = !setModifyInformation(observer, entity.getEntityId(), true);

        // Resend packets
        if (ProtocolLibrary.getProtocolManager() != null && hiddenBefore) {
            Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> ProtocolLibrary.getProtocolManager().updateEntity(entity, Collections.singletonList(observer)));
        }
    }

    /**
     * Determine if a given entity is visible for a particular observer.
     *
     * @param observer - the observer player.
     * @param entityID -  ID of the entity that we are testing for visibility.
     *
     * @return TRUE if the entity is visible, FALSE otherwise.
     */
    protected final boolean isInformationModified(final Player observer, final int entityID)
    {
        // If we are using a whitelist, presence means visibility - if not, the opposite is the case
        return getMembership(observer, entityID);
    }

    /**
     * Set the visibility status of a given entity for a particular observer.
     *
     * @param observer          - the observer player.
     * @param entityID          - ID of the entity that will be hidden or made visible.
     * @param modifyInformation - TRUE if the entity should be made visible, FALSE if not.
     *
     * @return TRUE if the entity was visible before this method call, FALSE otherwise.
     */
    protected final boolean setModifyInformation(final Player observer, final int entityID, final boolean modifyInformation)
    {
        // Non-membership means they are visible
        return !setMembership(observer, entityID, !modifyInformation);
    }

    /**
     * Prevent the observer from seeing a given entity.
     *
     * @param observer - the player observer.
     * @param entity   - the entity to hide.
     */
    public abstract void modifyInformation(Player observer, Entity entity);
}
