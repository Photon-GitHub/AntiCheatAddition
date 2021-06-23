package de.photon.aacadditionpro.util.packetwrappers;

import com.comphenix.protocol.utility.MinecraftReflection;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.exception.UnknownMinecraftException;
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
        switch (ServerVersion.getActiveServerVersion()) {
            case MC18:
            case MC112:
                return MessageChannel.of("minecraft", "placeholder", getHandle().getStrings().read(0));
            case MC113:
            case MC114:
            case MC115:
            case MC116:
            case MC117:
                return MessageChannel.of(getHandle().getMinecraftKeys().read(0));
            default:
                throw new UnknownMinecraftException();
        }
    }

    /**
     * Starting in 1.13, channel names need to be lower case, in the new identifier format,
     * i.e. {@code minecraft:brand}. The previously standard {@code |} is no longer allowed.
     */
    default void setChannel(KeyMessageChannel value)
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC18:
            case MC112:
                getHandle().getStrings().write(0, value.getChannel());
                break;
            case MC113:
            case MC114:
            case MC115:
            case MC116:
            case MC117:
                getHandle().getMinecraftKeys().write(0, value);
                break;
            default:
                throw new UnknownMinecraftException();
        }
    }

    /**
     * Starting in 1.13, channel names need to be lower case, in the new identifier format,
     * i.e. {@code minecraft:brand}. The previously standard {@code |} is no longer allowed.
     */
    default void setChannel(MessageChannel value)
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC18:
            case MC112:
                getHandle().getStrings().write(0, value.getChannel());
                break;
            case MC113:
            case MC114:
            case MC115:
            case MC116:
            case MC117:
                getHandle().getMinecraftKeys().write(0, (KeyMessageChannel) value);
                break;
            default:
                throw new UnknownMinecraftException();
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