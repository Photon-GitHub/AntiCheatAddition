package de.photon.AACAdditionPro.checks;

import de.photon.AACAdditionPro.events.ClientControlEvent;
import de.photon.AACAdditionPro.exceptions.NoViolationLevelManagementException;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.commands.CommandUtils;
import de.photon.AACAdditionPro.util.general.StringUtils;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.List;

public interface ClientControlModule extends ViolationModule
{
    String MC_BRAND_CHANNEL = "MC|Brand";

    List<String> getCommandsOnDetection();

    /**
     * This is used for the ClientControl checks as they do not need full thresholds
     *
     * @param player the player which has triggered ClientControl
     */
    default void executeCommands(final Player player)
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
            this.getCommandsOnDetection().forEach(rawCommand -> CommandUtils.executeCommandWithPlaceholders(rawCommand, player, this.getModuleType(), null));
        }
    }

    /**
     * Determines whether a channel is the MC_BRAND_CHANNEL.
     *
     * @param channel the channel which should be tested.
     */
    default boolean isBrandChannel(final String channel)
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
    default String getMCBrandMessage(final String channel, final byte[] message)
    {
        if (isBrandChannel(channel))
        {
            return new String(message, StandardCharsets.UTF_8);
        }
        return null;
    }

    /**
     * Tests if the MC|Brand message contains certain {@link String}s.
     */
    default boolean mcBrandMessageContains(final String channel, final byte[] message, final String[] flags)
    {
        final String brandMessage = getMCBrandMessage(channel, message);

        // Preconditions for StringUtils
        return (brandMessage != null && flags != null) &&
               StringUtils.stringContainsFlagsIgnoreCase(brandMessage, flags);
    }

    default boolean shouldFlagBrandCheck(final String channel, final Player player, final byte[] message, final String... flags)
    {
        final User user = UserManager.getUser(player.getUniqueId());

        if (User.isUserInvalid(user, this.getModuleType()))
        {
            return false;
        }

        // Bypassed players are already filtered out.
        boolean flag = true;

        if (isBrandChannel(channel))
        {
            flag = this.mcBrandMessageContains(channel, message, flags);
        }

        // Should flag
        return flag;
    }

    @Override
    default String getName()
    {
        return this.getConfigString().replace(".", "-");
    }

    @Override
    default ViolationLevelManagement getViolationLevelManagement()
    {
        throw new NoViolationLevelManagementException(this.getModuleType());
    }
}
