package de.photon.anticheataddition.util.pluginmessage.labymod;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.photon.anticheataddition.protocol.packetwrappers.sentbyserver.WrapperPlayServerCustomPayload;
import de.photon.anticheataddition.util.pluginmessage.MessageChannel;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;

@UtilityClass
public class LabyProtocolUtil
{
    /**
     * Send a tablist server banner to LabyMod.
     *
     * @param player   Minecraft Client
     * @param imageUrl the url of the image that shall be displayed on the client's tablist
     */
    public static void sendServerBanner(Player player, String imageUrl)
    {
        val object = new JsonObject();
        object.addProperty("url", imageUrl); // Url of the image
        sendLabyModMessage(player, "server_banner", object);
    }

    /**
     * Sends the permission message to the player.
     *
     * @see <a href="https://docs.labymod.net/pages/server/moderation/permissions/">https://docs.labymod.net/pages/server/moderation/permissions/</a>
     */
    public static void sendPermissionMessage(Player player)
    {
        sendLabyModMessage(player, "PERMISSIONS", LabyModPermission.getPermissionJsonObject());
    }

    /**
     * Just send this packet if the player joins the server to disallow the voice chat for the entire server.
     */
    public static void disableVoiceChat(Player player)
    {
        val object = new JsonObject();

        // Disable the voice chat for this player
        object.addProperty("allowed", false);

        // Send to LabyMod using the API
        sendLabyModMessage(player, "voicechat", object);
    }

    /**
     * Send a message to LabyMod
     *
     * @param player         Minecraft Client
     * @param key            LabyMod message key
     * @param messageContent json object content
     */
    public static void sendLabyModMessage(Player player, String key, JsonElement messageContent)
    {
        val payload = new WrapperPlayServerCustomPayload();
        payload.setContents(getBytesToSend(key, messageContent.toString()));
        payload.setChannel(MessageChannel.LABYMOD_CHANNEL);
        payload.sendPacket(player);
    }

    /**
     * Gets the bytes that are required to send the given message
     *
     * @param messageKey      the message's key
     * @param messageContents the message's contents
     *
     * @return the byte array that should be the payload
     */
    public static byte[] getBytesToSend(String messageKey, String messageContents)
    {
        // Getting an empty buffer
        ByteBuf byteBuf = Unpooled.buffer();

        // Writing the message-key to the buffer
        writeString(byteBuf, messageKey);

        // Writing the contents to the buffer
        writeString(byteBuf, messageContents);

        // Copying the buffer's bytes to the byte array
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);

        // Release the buffer
        byteBuf.release();

        // Returning the byte array
        return bytes;
    }

    /**
     * Writes a varint to the given byte buffer
     *
     * @param buf   the byte buffer the int should be written to
     * @param input the int that should be written to the buffer
     */
    private static void writeVarIntToBuffer(ByteBuf buf, int input)
    {
        while ((input & -128) != 0) {
            buf.writeByte(input & 127 | 128);
            input >>>= 7;
        }

        buf.writeByte(input);
    }

    /**
     * Writes a string to the given byte buffer
     *
     * @param buf    the byte buffer the string should be written to
     * @param string the string that should be written to the buffer
     */
    private static void writeString(ByteBuf buf, String string)
    {
        byte[] abyte = string.getBytes(StandardCharsets.UTF_8);

        if (abyte.length > Short.MAX_VALUE) throw new IllegalArgumentException("String too big (was " + string.length() + " bytes encoded, max " + Short.MAX_VALUE + ")");
        else {
            writeVarIntToBuffer(buf, abyte.length);
            buf.writeBytes(abyte);
        }
    }

    /**
     * Reads a varint from the given byte buffer
     *
     * @param buf the byte buffer the varint should be read from
     *
     * @return the int read
     */
    public static int readVarIntFromBuffer(ByteBuf buf)
    {
        int i = 0;
        int j = 0;

        byte b0;
        do {
            b0 = buf.readByte();
            i |= (b0 & 127) << j++ * 7;
            if (j > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while ((b0 & 128) == 128);

        return i;
    }

    /**
     * Reads a string from the given byte buffer
     *
     * @param buf       the byte buffer the string should be read from
     * @param maxLength the string's max-length
     *
     * @return the string read
     */
    public static String readString(ByteBuf buf, int maxLength)
    {
        int i = readVarIntFromBuffer(buf);

        if (i > maxLength * 4) throw new IllegalArgumentException("The received encoded string buffer length is longer than maximum allowed (" + i + " > " + maxLength * 4 + ")");
        else if (i < 0) throw new IllegalArgumentException("The received encoded string buffer length is less than zero! Weird string!");
        else {
            byte[] bytes = new byte[i];
            buf.readBytes(bytes);

            String s = new String(bytes, StandardCharsets.UTF_8);
            if (s.length() > maxLength) throw new IllegalArgumentException("The received string length is longer than maximum allowed (" + i + " > " + maxLength + ")");
            return s;
        }
    }
}
