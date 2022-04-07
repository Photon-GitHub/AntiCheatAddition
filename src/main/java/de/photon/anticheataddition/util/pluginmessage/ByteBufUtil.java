package de.photon.anticheataddition.util.pluginmessage;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@UtilityClass
public final class ByteBufUtil
{
    public static void writeString(ByteBuf buf, String s)
    {
        Preconditions.checkArgument(s.length() <= Short.MAX_VALUE, "Cannot send string longer than Short.MAX_VALUE (got " + s.length() + " characters)");

        final byte[] b = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(buf, b.length);
        buf.writeBytes(b);
    }

    /**
     * Reads a string from the given byte buffer
     *
     * @param buf the byte buffer the string should be read from
     *
     * @return the string read
     */
    public static String readString(ByteBuf buf)
    {
        final int len = readVarInt(buf);

        Preconditions.checkArgument(len >= 0, "String len smaller than zero.");
        Preconditions.checkArgument(len <= Short.MAX_VALUE, "Cannot receive string longer than Short.MAX_VALUE (got " + len + " characters)");

        final byte[] b = new byte[len];
        buf.readBytes(b);

        return new String(b, StandardCharsets.UTF_8);
    }

    public static void writeArray(ByteBuf buf, byte[] b)
    {
        Preconditions.checkArgument(b.length <= Short.MAX_VALUE, "Cannot send byte array longer than Short.MAX_VALUE (got " + b.length + " bytes)");

        writeVarInt(buf, b.length);
        buf.writeBytes(b);
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
        final int len = readVarInt(buf);

        Preconditions.checkArgument(len <= limit, "Cannot receive byte array longer than " + limit + " (got " + len + " bytes)");
        final byte[] ret = new byte[len];
        buf.readBytes(ret);
        return ret;
    }

    public static void writeStringArray(List<String> s, ByteBuf buf)
    {
        writeVarInt(buf, s.size());
        for (String str : s) writeString(buf, str);
    }

    public static List<String> readStringArray(ByteBuf buf)
    {
        final int len = readVarInt(buf);

        final String[] array = new String[len];
        for (int i = 0; i < array.length; ++i) array[i] = readString(buf);
        return List.of(array);
    }

    public static int readVarInt(ByteBuf input)
    {
        return readVarInt(input, 5);
    }

    /**
     * Reads a variable int from the given byte buffer
     *
     * @param input    the byte buffer the varint should be read from.
     * @param maxBytes the maximum amount of bytes read.
     *
     * @return the int read
     */
    public static int readVarInt(ByteBuf input, int maxBytes)
    {
        int out = 0;
        int bytes = 0;
        byte in;
        do {
            in = input.readByte();

            out |= (in & 0x7F) << (bytes++ * 7);

            if (bytes > maxBytes) throw new IllegalArgumentException("VarInt too big.");
        } while ((in & 0x80) == 0x80);

        return out;
    }

    /**
     * Writes a variable int to the given byte buffer
     *
     * @param output the byte buffer the int should be written to
     * @param value  the int that should be written to the buffer
     */
    public static void writeVarInt(ByteBuf output, int value)
    {
        int part;
        do {
            // Get the first 7 bits into part.
            part = value & 0x7F;
            value >>>= 7;

            // If the int is not fully written, add the 8th bit.
            if (value != 0) part |= 0x80;

            output.writeByte(part);
        } while (value != 0);
    }

    public static int readVarShort(ByteBuf buf)
    {
        int low = buf.readUnsignedShort();
        int high = 0;
        if ((low & 0x8000) != 0) {
            low &= 0x7FFF;
            high = buf.readUnsignedByte();
        }
        return ((high & 0xFF) << 15) | low;
    }

    public static void writeVarShort(ByteBuf buf, int toWrite)
    {
        final int high = (toWrite & 0x7F8000) >> 15;
        int low = toWrite & 0x7FFF;
        if (high != 0) low |= 0x8000;

        buf.writeShort(low);
        if (high != 0) buf.writeByte(high);
    }

    public static void writeUUID(UUID value, ByteBuf output)
    {
        output.writeLong(value.getMostSignificantBits());
        output.writeLong(value.getLeastSignificantBits());
    }

    public static UUID readUUID(ByteBuf input)
    {
        return new UUID(input.readLong(), input.readLong());
    }
}