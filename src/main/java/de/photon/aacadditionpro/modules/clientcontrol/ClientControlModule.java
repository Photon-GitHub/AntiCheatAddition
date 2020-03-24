package de.photon.aacadditionpro.modules.clientcontrol;

import de.photon.aacadditionpro.events.ClientControlEvent;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.olduser.UserManager;
import de.photon.aacadditionpro.olduser.UserOld;
import de.photon.aacadditionpro.util.commands.CommandUtils;
import de.photon.aacadditionpro.util.files.configs.ConfigUtils;
import de.photon.aacadditionpro.util.general.StringUtils;
import de.photon.aacadditionpro.util.pluginmessage.MessageChannel;
import org.bukkit.entity.Player;

import java.util.List;

abstract class ClientControlModule implements Module
{
    // All the commands executed upon detection
    // If a module does not have commands to execute this will just be an empty list.
    private final List<String> commandsOnDetection = ConfigUtils.loadStringOrStringList(this.getConfigString() + ".commands_on_detection");

    /**
     * This is used for the ClientControl checks as they do not need full thresholds
     *
     * @param player the player which has triggered ClientControl
     */
    void executeCommands(final Player player)
    {
        ClientControlEvent.build(player, this.getModuleType())
                          // Call the event
                          .call()
                          // Execution of the commands if the event is not cancelled.
                          .runIfUncancelled(event -> {
                              for (String rawCommand : this.commandsOnDetection) {
                                  CommandUtils.executeCommandWithPlaceholders(rawCommand, player, this.getModuleType());
                              }
                          });
    }

    /**
     * Determines whether a channel is the MC_BRAND_CHANNEL.
     *
     * @param channel the channel which should be tested.
     */
    boolean isBrandChannel(final String channel)
    {
        return channel.equals(MessageChannel.MC_BRAND_CHANNEL.getChannel());
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
        return isBrandChannel(channel) ?
               StringUtils.fromUTF8Bytes(message) :
               null;
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
        final UserOld user = UserManager.getUser(player.getUniqueId());

        return !UserOld.isUserInvalid(user, this.getModuleType()) &&
               this.isBrandChannel(channel) &&
               this.mcBrandMessageContains(channel, message, flags);
    }
}
