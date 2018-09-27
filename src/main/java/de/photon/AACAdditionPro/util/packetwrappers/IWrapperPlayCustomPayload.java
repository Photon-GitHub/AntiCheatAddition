package de.photon.AACAdditionPro.util.packetwrappers;

import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.MinecraftKey;
import de.photon.AACAdditionPro.ServerVersion;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public interface IWrapperPlayCustomPayload extends IWrapperPlay
{
    /**
     * Retrieve Channel.
     * <p>
     * Notes: name of the "channel" used to send the data.
     *
     * @return The current Channel
     */
    default MinecraftKey getChannel()
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
            case MC111:
            case MC112:
                return new MinecraftKey(getHandle().getStrings().read(0));
            case MC113:
                return getHandle().getMinecraftKeys().read(0);
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }

    /**
     * Starting in 1.13, channel names need to be lower case, in the new identifier format,
     * i.e. {@code minecraft:brand}. The previously standard {@code |} is no longer allowed.
     */
    default void setChannel(MinecraftKey value)
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
            case MC111:
            case MC112:
                getHandle().getStrings().write(0, value.getKey());
                break;
            case MC113:
                getHandle().getMinecraftKeys().write(0, value);
                break;
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }

    /**
     * Retrieve payload contents as a raw Netty buffer
     *
     * @return Payload contents as a Netty buffer
     */
    default ByteBuf getContentsBuffer()
    {
        return (ByteBuf) getHandle().getModifier().withType(ByteBuf.class).read(0);
    }

    /**
     * Retrieve payload contents
     *
     * @return Payload contents as a byte array
     */
    default byte[] getContents()
    {
        ByteBuf buffer = getContentsBuffer();
        byte[] array = new byte[buffer.readableBytes()];
        buffer.readBytes(array);
        return array;
    }

    /**
     * Update payload contents with a Netty buffer
     *
     * @param contents - new payload content
     */
    default void setContentsBuffer(ByteBuf contents)
    {
        if (MinecraftReflection.is(MinecraftReflection.getPacketDataSerializerClass(), contents)) {
            getHandle().getModifier().withType(ByteBuf.class).write(0, contents);
        }
        else {
            Object serializer = MinecraftReflection.getPacketDataSerializer(contents);
            getHandle().getModifier().withType(ByteBuf.class).write(0, serializer);
        }
    }

    /**
     * Update payload contents with a byte array
     *
     * @param content - new payload content
     */
    default void setContents(byte[] content)
    {
        setContentsBuffer(Unpooled.copiedBuffer(content));
    }
}