package de.photon.AACAdditionPro.checks;

import de.photon.AACAdditionPro.events.ClientControlEvent;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.commands.CommandUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.UnsupportedEncodingException;
import java.util.Collection;

public interface ClientControlModule extends ViolationModule
{
    String MCBRANDCHANNEL = "MC|Brand";

    Collection<String> getCommandsOnDetection();

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

    static boolean isBranded(final String channel)
    {
        return channel.equals(MCBRANDCHANNEL);
    }

    /**
     * This is used to get the message which is encoded in the MC|Brand channel
     *
     * @param channel the channel the message is delivered with. Used to determine if the current channel is MC|Brand
     * @param message the message is encoded as a byte array and will be decoded to a {@link String} in this method
     *
     * @return the decoded message or null if either the channel was not MC|Brand or there was a problem while decoding the message.
     */
    static String getBrand(final String channel, final byte[] message)
    {
        if (isBranded(channel))
        {
            try
            {
                return new String(message, "UTF-8");
            } catch (final UnsupportedEncodingException e)
            {
                System.out.println("Unable to encode message.");
                e.printStackTrace();
            }
        }
        return null;
    }

    static boolean shouldFlagBrandCheck(final String channel, final Player player, final byte[] message, final String[] flags)
    {
        final User user = UserManager.getUser(player.getUniqueId());

        if (User.isUserInvalid(user))
        {
            return false;
        }

        // Bypassed players are already filtered out.
        boolean flag = true;

        // MC-Brand for vanilla world-downloader
        if (ClientControlModule.isBranded(channel))
        {
            flag = ClientControlModule.brandContains(channel, message, flags);
        }

        // Should flag
        return flag;
    }

    static boolean brandContains(final String channel, final byte[] message, final String[] flags)
    {
        return stringContainsFlag(getBrand(channel, message), flags);
    }

    static boolean stringContainsFlag(String input, final String[] flags)
    {
        if (input == null || flags == null)
        {
            return false;
        }

        input = input.toLowerCase();

        for (final String flag : flags)
        {
            final String lowerflag = flag.toLowerCase();

            if (input.contains(lowerflag))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    default String getName()
    {
        return this.getConfigString().replace(".", "-");
    }
}
