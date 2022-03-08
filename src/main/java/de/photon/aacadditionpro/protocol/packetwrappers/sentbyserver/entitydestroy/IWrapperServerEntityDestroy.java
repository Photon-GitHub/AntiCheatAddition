package de.photon.aacadditionpro.protocol.packetwrappers.sentbyserver.entitydestroy;

import com.comphenix.protocol.PacketType;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.protocol.packetwrappers.IWrapperPlayEntity;

import java.util.List;

public interface IWrapperServerEntityDestroy extends IWrapperPlayEntity
{
    PacketType TYPE = PacketType.Play.Server.ENTITY_DESTROY;

    static IWrapperServerEntityDestroy create()
    {
        return ServerVersion.containsActiveServerVersion(ServerVersion.MC117.getSupVersionsTo()) ?
               new LegacyWrapperPlayServerEntityDestroy() : new ModernWrapperPlayServerEntityDestroy();
    }

    /**
     * Retrieve Count.
     * <p>
     * Notes: length of following array
     *
     * @return The current Count
     */
    default int getCount()
    {
        return getEntityIDs().size();
    }

    /**
     * Retrieve Entity IDs.
     * <p>
     * Notes: the list of entities of destroy
     *
     * @return The current Entity IDs
     */
    List<Integer> getEntityIDs();

    /**
     * Set Entity IDs.
     *
     * @param value - new value.
     */
    void setEntityIds(List<Integer> value);
}
