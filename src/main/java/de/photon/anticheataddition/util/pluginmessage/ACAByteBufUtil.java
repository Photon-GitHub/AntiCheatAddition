package de.photon.anticheataddition.util.pluginmessage;

import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;

@UtilityClass
public final class ACAByteBufUtil
{
    private static final int MAX_STRING_LENGTH = Short.MAX_VALUE;
    private static final int MAX_STRING_BYTES = MAX_STRING_LENGTH * 3;

    public static void writeString(ByteBuf buf, String s)
    {
        final int size = ByteBufUtil.utf8Bytes(s);
        ByteBufHelper.writeVarInt(buf, size);
        buf.writeCharSequence(s, StandardCharsets.UTF_8);
    }

    /**
     * Reads a string from the given byte buffer
     *
     * @param buf the byte buffer the string should be read from
     * @return the string read
     */
    public static String readString(ByteBuf buf)
    {
        final int length = ByteBufHelper.readVarInt(buf);

        if (length < 0) throw new IllegalArgumentException("String len smaller than zero.");
        if (length > MAX_STRING_BYTES) throw new IllegalArgumentException("Cannot receive string longer than " + MAX_STRING_BYTES + " bytes (got " + length + " bytes)");

        return buf.readString(length, StandardCharsets.UTF_8);
    }

    public static void writeByteArray(ByteBuf buf, byte[] bytes)
    {
        ByteBufHelper.writeVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    public static byte[] toArray(ByteBuf buf)
    {
        final byte[] ret = new byte[buf.readableBytes()];
        buf.readBytes(ret);
        return ret;
    }

    public static byte[] readArray(ByteBuf buf)
    {
        return readArray(buf, buf.readableBytes());
    }

    public static byte[] readArray(ByteBuf buf, int limit)
    {
        if (limit < 0) throw new IllegalArgumentException("Byte array limit smaller than zero.");

        final int len = ByteBufHelper.readVarInt(buf);

        if (len < 0) throw new IllegalArgumentException("Byte array len smaller than zero.");

        final int available = Math.min(limit, buf.readableBytes());
        if (len > available) throw new IllegalArgumentException("Cannot receive byte array longer than " + available + " (got " + len + " bytes)");

        final byte[] ret = new byte[len];
        buf.readBytes(ret);
        return ret;
    }
}
