package de.photon.anticheataddition.util.pluginmessage.labymod;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.photon.anticheataddition.util.pluginmessage.ByteBufUtil;
import de.photon.anticheataddition.util.pluginmessage.MessageChannel;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.experimental.UtilityClass;
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
        final var object = new JsonObject();
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
        sendLabyModMessage(player, "PERMISSIONS", LabyModPermission.getPermissionsJson());
    }

    /**
     * Just send this packet if the player joins the server to disallow the voice chat for the entire server.
     */
    public static void disableVoiceChat(Player player)
    {
        final var object = new JsonObject();

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
        final var channel = MessageChannel.LABYMOD_CHANNEL.getChannel().orElseThrow(() -> new IllegalStateException("LabyMod channel not found"));
        final var messageWrapper = new WrapperPlayServerPluginMessage(channel, getBytesToSend(key, messageContent.toString()));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, messageWrapper);
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
        final byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);

        // Release the buffer
        byteBuf.release();

        // Returning the byte array
        return bytes;
    }
}
