package de.photon.anticheataddition.user.data.subdata;

import de.photon.anticheataddition.modules.sentinel.ParsedPluginMessageListener;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.datastructure.buffer.RingBuffer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a data class to manage and save brand channel messages.
 */
public class BrandChannelData
{
    // Use a RingBuffer here as a malicious client could spam the brand channel with messages.
    private final RingBuffer<String> brandChannelMessages = new RingBuffer<>(10);

    // This method is private as only the listener below is supposed to add messages.
    private void addBrandChannelMessage(String message)
    {
        brandChannelMessages.add(message);
    }

    public boolean hasBrandChannelMessage(String message)
    {
        return brandChannelMessages.contains(message);
    }

    public String listBrandChannelMessages()
    {
        return String.join(", ", brandChannelMessages);
    }

    /**
     * Listens for brand channel plugin messages and updates the {@link User}'s {@link BrandChannelData} accordingly.
     */
    public static class BrandChannelMessageListener implements ParsedPluginMessageListener
    {
        @Override
        public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull String message)
        {
            final var user = User.getUser(player);
            if (user == null) return;

            user.getBrandChannelData().addBrandChannelMessage(message);
        }
    }
}
