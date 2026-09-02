package de.photon.anticheataddition.util.pluginmessage;

import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

class ACAByteBufUtilTest
{
    @Test
    void stringLengthPrefixIsTheUtf8ByteLength()
    {
        final String value = "€".repeat(Short.MAX_VALUE);
        final ByteBuf buf = Unpooled.buffer();
        try {
            ACAByteBufUtil.writeString(buf, value);
            Assertions.assertEquals(value.getBytes(StandardCharsets.UTF_8).length, ByteBufHelper.readVarInt(buf));
            buf.readerIndex(0);
            Assertions.assertEquals(value, ACAByteBufUtil.readString(buf));
        } finally {
            buf.release();
        }
    }

    @Test
    void byteArrayDoesNotUseAProtocolIndependentShortLimit()
    {
        final byte[] value = new byte[Short.MAX_VALUE + 1];
        final ByteBuf buf = Unpooled.buffer();
        try {
            ACAByteBufUtil.writeByteArray(buf, value);
            Assertions.assertArrayEquals(value, ACAByteBufUtil.readArray(buf));
        } finally {
            buf.release();
        }
    }

    @Test
    void stringUsesMinecraftDefaultCharacterLimit()
    {
        final ByteBuf buf = Unpooled.buffer();
        try {
            Assertions.assertThrows(IllegalArgumentException.class, () -> ACAByteBufUtil.writeString(buf, "a".repeat(Short.MAX_VALUE + 1)));

            ByteBufHelper.writeVarInt(buf, Short.MAX_VALUE + 1);
            buf.writeZero(Short.MAX_VALUE + 1);
            Assertions.assertThrows(IllegalArgumentException.class, () -> ACAByteBufUtil.readString(buf));
        } finally {
            buf.release();
        }
    }

    @Test
    void stringRejectsAnOversizedEncodedPayload()
    {
        final ByteBuf buf = Unpooled.buffer();
        try {
            ByteBufHelper.writeVarInt(buf, Short.MAX_VALUE * 3 + 1);
            Assertions.assertThrows(IllegalArgumentException.class, () -> ACAByteBufUtil.readString(buf));
        } finally {
            buf.release();
        }
    }

    @Test
    void arrayRejectsNegativeAndTruncatedLengths()
    {
        final ByteBuf negative = Unpooled.buffer();
        final ByteBuf truncated = Unpooled.buffer();
        try {
            ByteBufHelper.writeVarInt(negative, -1);
            Assertions.assertThrows(IllegalArgumentException.class, () -> ACAByteBufUtil.readArray(negative));

            ByteBufHelper.writeVarInt(truncated, 2);
            truncated.writeByte(1);
            Assertions.assertThrows(IllegalArgumentException.class, () -> ACAByteBufUtil.readArray(truncated));
        } finally {
            negative.release();
            truncated.release();
        }
    }
}
