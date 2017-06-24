package de.photon.AACAdditionPro.util.packetwrappers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.utility.MinecraftReflection;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class WrapperPlayServerCustomPayload extends AbstractPacket {
    public static final PacketType TYPE = PacketType.Play.Server.CUSTOM_PAYLOAD;

    public WrapperPlayServerCustomPayload() {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayServerCustomPayload(final PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieve Channel.
     * <p>
     * Notes: name of the "channel" used to send the data.
     *
     * @return The current Channel
     */
    public String getChannel() {
        return handle.getStrings().read(0);
    }

    /**
     * Set Channel.
     *
     * @param value - new value.
     */
    public void setChannel(final String value) {
        handle.getStrings().write(0, value);
    }

    /**
     * Retrieve payload contents as a raw Netty buffer
     *
     * @return Payload contents as a Netty buffer
     */
    public ByteBuf getContentsBuffer() {
        return (ByteBuf) handle.getModifier().withType(ByteBuf.class).read(0);
    }

    /**
     * Retrieve payload contents
     *
     * @return Payload contents as a byte array
     */
    public byte[] getContents() {
        final ByteBuf buffer = getContentsBuffer();
        final byte[] array = new byte[buffer.readableBytes()];
        buffer.readBytes(array);
        return array;
    }

    /**
     * Update payload contents with a Netty buffer
     *
     * @param contents - new payload content
     */
    public void setContentsBuffer(final ByteBuf contents) {
        if (MinecraftReflection.is(MinecraftReflection.getPacketDataSerializerClass(), contents)) {
            handle.getModifier().withType(ByteBuf.class).write(0, contents);
        } else {
            final Object serializer = MinecraftReflection.getPacketDataSerializer(contents);
            handle.getModifier().withType(ByteBuf.class).write(0, serializer);
        }
    }

    /**
     * Update payload contents with a byte array
     *
     * @param content - new payload content
     */
    public void setContents(final byte[] content) {
        setContentsBuffer(Unpooled.copiedBuffer(content));
    }
}