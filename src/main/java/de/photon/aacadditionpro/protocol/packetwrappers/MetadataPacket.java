package de.photon.aacadditionpro.protocol.packetwrappers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.exception.UnknownMinecraftException;
import de.photon.aacadditionpro.protocol.EntityMetadataIndex;
import lombok.val;

import java.util.List;

public abstract class MetadataPacket extends AbstractPacket
{
    /**
     * Constructs a new strongly typed wrapper for the given packet.
     *
     * @param handle - handle to the raw packet data.
     * @param type   - the packet type.
     */
    protected MetadataPacket(PacketContainer handle, PacketType type)
    {
        super(handle, type);
    }

    public abstract List<WrappedWatchableObject> getRawMetadata();

    /**
     * Searches for an index in the metadata as it is unsorted.
     *
     * @return the {@link WrappedWatchableObject}.
     *
     * @throws IllegalArgumentException if the index was not found.
     */
    public WrappedWatchableObject getMetadataIndex(int index)
    {
        val rawMetadata = getRawMetadata();
        for (WrappedWatchableObject watch : rawMetadata) {
            if (watch.getIndex() == index) {
                return watch;
            }
        }
        throw new IllegalArgumentException("Index " + index + " could not be found in entity metadata.");
    }

    public MetadataBuilder builder()
    {
        return new MetadataBuilder();
    }

    public static class MetadataBuilder
    {
        final WrappedDataWatcher dataWatcher = new WrappedDataWatcher();

        // ------------------------------------------------- Entity ------------------------------------------------- //

        /**
         * Sets a metadata.
         */
        public MetadataBuilder setMetadata(final int index, final Class classOfValue, final Object value)
        {
            switch (ServerVersion.getActiveServerVersion()) {
                case MC18:
                    dataWatcher.setObject(index, value);
                    break;
                case MC112:
                case MC113:
                case MC114:
                case MC115:
                case MC116:
                case MC117:
                    dataWatcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(index, WrappedDataWatcher.Registry.get(classOfValue)), value);
                    break;
                default:
                    throw new UnknownMinecraftException();
            }

            return this;
        }

        /**
         * Sets the zero index metadata, which can be used for several settings.
         *
         * @see <a href="https://wiki.vg/Entity_metadata#Entity">https://wiki.vg/Entity_metadata#Entity</a>
         */
        public MetadataBuilder setZeroIndex(final byte zeroByte)
        {
            return this.setMetadata(0, Byte.class, zeroByte);
        }


        // ------------------------------------------------- Living ------------------------------------------------- //

        /**
         * Sets the health metadata.
         *
         * @see <a href="https://wiki.vg/Entity_metadata#Living">https://wiki.vg/Entity_metadata#Living</a>
         */
        public MetadataBuilder setHealthMetadata(final float health)
        {
            return this.setMetadata(EntityMetadataIndex.HEALTH, Float.class, health);
        }

        /**
         * Sets the arrows in entity metadata.
         *
         * @see <a href="https://wiki.vg/Entity_metadata#Living">https://wiki.vg/Entity_metadata#Living</a>
         */
        public MetadataBuilder setArrowInEntityMetadata(final int arrows)
        {
            return ServerVersion.getActiveServerVersion() == ServerVersion.MC18 ?
                   // IN 1.8.8 THIS IS A BYTE, NOT AN INTEGER!
                   this.setMetadata(EntityMetadataIndex.ARROWS_IN_ENTITY, Byte.class, (byte) arrows) :
                   this.setMetadata(EntityMetadataIndex.ARROWS_IN_ENTITY, Integer.class, arrows);
        }

        // ------------------------------------------------- Player ------------------------------------------------- //

        /**
         * Sets the skin part metadata
         *
         * @see <a href="https://wiki.vg/Entity_metadata#Player">https://wiki.vg/Entity_metadata#Player</a>
         */
        public MetadataBuilder setSkinMetadata(final byte skinParts)
        {
            return this.setMetadata(EntityMetadataIndex.SKIN_PARTS, Byte.class, skinParts);
        }

        /**
         * Get the metadata in form of a {@link WrappedDataWatcher}
         */
        public WrappedDataWatcher asWatcher()
        {
            return this.dataWatcher;
        }

        /**
         * Get the metadata in form of a {@link List} of {@link WrappedWatchableObject}s.
         */
        public List<WrappedWatchableObject> asList()
        {
            return this.dataWatcher.getWatchableObjects();
        }
    }
}
