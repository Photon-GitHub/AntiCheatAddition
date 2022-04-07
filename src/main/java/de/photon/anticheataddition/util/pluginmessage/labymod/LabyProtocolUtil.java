package de.photon.anticheataddition.util.pluginmessage.labymod;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.photon.anticheataddition.protocol.packetwrappers.sentbyserver.WrapperPlayServerCustomPayload;
import de.photon.anticheataddition.util.pluginmessage.ByteBufUtil;
import de.photon.anticheataddition.util.pluginmessage.MessageChannel;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.bukkit.entity.Player;

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
        final ByteBuf byteBuf = Unpooled.buffer();

        // Writing the message-key to the buffer
        ByteBufUtil.writeString(byteBuf, messageKey);

        // Writing the contents to the buffer
        ByteBufUtil.writeString(byteBuf, messageContents);

        // Copying the buffer's bytes to the byte array
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);

        // Release the buffer
        byteBuf.release();

        // Returning the byte array
        return bytes;
    }
}
