package de.photon.AACAdditionPro.util.visibility;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.util.multiversion.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class PlayerInformationModifier
{
    protected final ProtocolManager manager = ProtocolLibrary.getProtocolManager();
    private final Table<Integer, Integer, Boolean> observerEntityMap = HashBasedTable.create();

    /**
     * Construct a new InformationModifier
     */
    protected PlayerInformationModifier()
    {
        // Only start if the ServerVersion is supported
        if (ServerVersion.supportsActiveServerVersion(this.getSupportedVersions()))
        {
            // Register events and packet listener
            AACAdditionPro.getInstance().registerListener(new Listener()
            {
                @EventHandler
                public void on(final EntityDeathEvent event)
                {
                    Bukkit.getScheduler().callSyncMethod(AACAdditionPro.getInstance(), () -> {
                        removeEntity(event.getEntity());
                        return null;
                    });
                }

                @EventHandler
                public void on(final ChunkUnloadEvent event)
                {
                    // Cache entities for performance reasons so the server doesn't need to load them again when the
                    // task is executed.
                    final Entity[] entities = event.getChunk().getEntities();
                    Bukkit.getScheduler().callSyncMethod(AACAdditionPro.getInstance(), () -> {
                        for (final Entity entity : entities)
                        {
                            removeEntity(entity);
                        }
                        return null;
                    });
                }

                @EventHandler
                public void on(final PlayerQuitEvent event)
                {
                    removePlayer(event.getPlayer());
                }
            });

            this.manager.addPacketListener(new PacketAdapter(AACAdditionPro.getInstance(), ListenerPriority.NORMAL, this.getAffectedPackets())
            {
                @Override
                public void onPacketSending(final PacketEvent event)
                {
                    final int entityID = event.getPacket().getIntegers().read(0);

                    // See if this packet should be cancelled
                    if (isInformationModified(event.getPlayer(), entityID))
                    {
                        event.setCancelled(true);
                    }
                }
            });
        }
    }

    protected abstract PacketType[] getAffectedPackets();

    protected Set<ServerVersion> getSupportedVersions()
    {
        return new HashSet<>(Arrays.asList(ServerVersion.values()));
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
        try
        {
            return observerEntityMap.contains(observer.getEntityId(), entityID);
        } catch (final UnsupportedOperationException ignore)
        {
            // Nothing here as this is a ProtocolLib problem with it's temporal players who have less methods
            // due to reflection.
        }
        return false;
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
        try
        {
            if (member)
            {
                return observerEntityMap.put(observer.getEntityId(), entityID, true) != null;
            }
            else
            {
                return observerEntityMap.remove(observer.getEntityId(), entityID) != null;
            }
        } catch (final UnsupportedOperationException ignore)
        {
            // Nothing here as this is a ProtocolLib problem with it's temporal players who have less methods
            // due to reflection.
        }
        return false;
    }

    /**
     * Remove the given entity from the underlying map.
     *
     * @param entity - the entity to remove.
     */
    private void removeEntity(final Entity entity)
    {
        try
        {
            final int entityID = entity.getEntityId();

            for (final Map<Integer, Boolean> maps : observerEntityMap.rowMap().values())
            {
                maps.remove(entityID);
            }
        } catch (final UnsupportedOperationException ignore)
        {
            // Nothing here as this is a ProtocolLib problem with it's temporal players who have less methods
            // due to reflection.
        }
    }

    /**
     * Invoked when a player logs out.
     *
     * @param player - the player that just logged out.
     */
    private void removePlayer(final Player player)
    {
        try
        {
            // Cleanup
            observerEntityMap.rowMap().remove(player.getEntityId());
        } catch (final UnsupportedOperationException ignore)
        {
            // Nothing here as this is a ProtocolLib problem with it's temporal players who have less methods
            // due to reflection.
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
        try
        {
            validate(observer, entity);
            final boolean hiddenBefore = !setModifyInformation(observer, entity.getEntityId(), true);

            // Resend packets
            if (manager != null && hiddenBefore)
            {
                manager.updateEntity(entity, Collections.singletonList(observer));
            }
        } catch (final UnsupportedOperationException ignore)
        {
            // Nothing here as this is a ProtocolLib problem with it's temporal players who have less methods
            // due to reflection.
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

    // For validating the input parameters
    protected static void validate(final Player observer, final Entity entity)
    {
        Preconditions.checkNotNull(observer, "observer cannot be NULL.");
        Preconditions.checkNotNull(entity, "entity cannot be NULL.");
    }

    /**
     * Prevent the observer from seeing a given entity.
     *
     * @param observer - the player observer.
     * @param entity   - the entity to hide.
     */
    public abstract void modifyInformation(Player observer, Entity entity);

// --Commented out by Inspection START (28.02.2017 21:25):
//    /**
//     * Determine if the given entity has been hidden from an observer.
//     * <p>
//     * Note that the entity may very well be occluded or out of range from the perspective
//     * of the observer. This method simply checks if an entity has been completely hidden
//     * for that observer.
//     * @param observer - the observer.
//     * @param entity - the entity that may be hidden.
//     * @return TRUE if the player may see the entity, FALSE if the entity has been hidden.
//     */
//    public final boolean isInformationUnModified(Player observer, Entity entity) {
//        validate(observer, entity);
//        return isInformationUnmodified(observer, entity.getEntityId());
//    }
// --Commented out by Inspection STOP (28.02.2017 21:25)
}
