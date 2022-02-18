package de.photon.aacadditionpro.protocol.packetwrappers;

import com.comphenix.protocol.utility.MinecraftReflection;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.util.pluginmessage.KeyMessageChannel;
import de.photon.aacadditionpro.util.pluginmessage.MessageChannel;
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
    default MessageChannel getChannel()
    {
        return ServerVersion.MC113.getVersionsTo().contains(ServerVersion.getActiveServerVersion()) ?
               MessageChannel.of("minecraft", "placeholder", getHandle().getStrings().read(0)) :
               MessageChannel.of(getHandle().getMinecraftKeys().read(0));
    }

    /**
     * Starting in 1.13, channel names need to be lower case, in the new identifier format,
     * i.e. {@code minecraft:brand}. The previously standard {@code |} is no longer allowed.
     */
    default void setChannel(MessageChannel value)
    {
        if (ServerVersion.MC113.getVersionsTo().contains(ServerVersion.getActiveServerVersion())) {
            value.getChannel().ifPresent(channel -> getHandle().getStrings().write(0, channel));
        } else {
            getHandle().getMinecraftKeys().write(0, (KeyMessageChannel) value);
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
     * Update payload contents with a Netty buffer
     *
     * @param contents - new payload content
     */
    default void setContentsBuffer(ByteBuf contents)
    {
        if (MinecraftReflection.is(MinecraftReflection.getPacketDataSerializerClass(), contents)) {
            getHandle().getModifier().withType(ByteBuf.class).write(0, contents);
        } else {
            Object serializer = MinecraftReflection.getPacketDataSerializer(contents);
            getHandle().getModifier().withType(ByteBuf.class).write(0, serializer);
        }
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
     * Update payload contents with a byte array
     *
     * @param content - new payload content
     */
    default void setContents(byte[] content)
    {
        setContentsBuffer(Unpooled.copiedBuffer(content));
    }
}