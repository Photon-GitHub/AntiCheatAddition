package de.photon.AACAdditionPro.modules.clientcontrol;

import de.photon.AACAdditionPro.events.ClientControlEvent;
import de.photon.AACAdditionPro.modules.Module;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.commands.CommandUtils;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.general.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

abstract class ClientControlModule implements Module
{
    static final String MC_BRAND_CHANNEL = "MC|Brand";

    // All the commands executed upon detection
    // If a module does not have commands to execute this will just be an empty list.
    @LoadFromConfiguration(configPath = ".commands_on_detection", listType = String.class)
    private List<String> commandsOnDetection;

    /**
     * This is used for the ClientControl checks as they do not need full thresholds
     *
     * @param player the player which has triggered ClientControl
     */
    void executeCommands(final Player player)
    {
        // Call the event
        final ClientControlEvent clientControlEvent = new ClientControlEvent(
                player,
                this.getModuleType()
        );

        Bukkit.getPluginManager().callEvent(clientControlEvent);

        // The event must not be cancelled
        if (!clientControlEvent.isCancelled())
        {
            // Execution of the commands
            this.commandsOnDetection.forEach(rawCommand -> CommandUtils.executeCommandWithPlaceholders(rawCommand, player, this.getModuleType(), null));
        }
    }

    /**
     * Determines whether a channel is the MC_BRAND_CHANNEL.
     *
     * @param channel the channel which should be tested.
     */
    boolean isBrandChannel(final String channel)
    {
        return channel.equals(MC_BRAND_CHANNEL);
    }

    /**
     * This is used to get the message which is encoded in the MC|Brand channel
     *
     * @param channel the channel the message is delivered with. Used to determine if the current channel is MC|Brand
     * @param message the message is encoded as a byte array and will be decoded to a {@link String} in this method
     *
     * @return the decoded message or null if either the channel was not MC|Brand or there was a problem while decoding the message.
     */
    String getMCBrandMessage(final String channel, final byte[] message)
    {
        return isBrandChannel(channel) ? StringUtils.fromUTF8Bytes(message) : null;
    }

    /**
     * Tests if the MC|Brand message contains certain {@link String}s.
     */
    boolean mcBrandMessageContains(final String channel, final byte[] message, final String[] flags)
    {
        final String brandMessage = getMCBrandMessage(channel, message);

        // Preconditions for StringUtils
        return (brandMessage != null && flags != null) &&
               StringUtils.stringContainsFlagsIgnoreCase(brandMessage, flags);
    }

    boolean shouldFlagBrandCheck(final String channel, final Player player, final byte[] message, final String... flags)
    {
        final User user = UserManager.getUser(player.getUniqueId());

        return !User.isUserInvalid(user, this.getModuleType()) &&
               this.isBrandChannel(channel) &&
               this.mcBrandMessageContains(channel, message, flags);
    }
}
