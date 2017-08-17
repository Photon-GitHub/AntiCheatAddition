package de.photon.AACAdditionPro.checks;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.events.ClientControlEvent;
import de.photon.AACAdditionPro.events.PlayerAdditionViolationCommandEvent;
import de.photon.AACAdditionPro.util.commands.CommandUtils;
import de.photon.AACAdditionPro.util.commands.Placeholders;
import org.bukkit.entity.Player;

import java.io.UnsupportedEncodingException;
import java.util.List;

public interface ClientControlCheck extends AACAdditionProCheck
{
    String MCBRANDCHANNEL = "MC|Brand";

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
                this.getAdditionHackType()
        );

        AACAdditionPro.getInstance().getServer().getPluginManager().callEvent(clientControlEvent);

        // The event must not be cancelled
        if (!clientControlEvent.isCancelled()) {

            // Execution of the commands
            for (final String rawCommand : this.getCommandsOnDetection()) {
                final String realCommand = Placeholders.applyPlaceholders(rawCommand, player);

                // Calling of the event + Sync command execution
                CommandUtils.executeCommand(new PlayerAdditionViolationCommandEvent(player, realCommand, this.getAdditionHackType()));
            }
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
        if (isBranded(channel)) {
            try {
                return new String(message, "UTF-8");
            } catch (final UnsupportedEncodingException e) {
                System.out.println("Unable to encode message.");
                e.printStackTrace();
            }
        }
        return null;
    }

    static boolean brandContains(final String channel, final byte[] message, final String[] flags)
    {
        return stringContainsFlag(getBrand(channel, message), flags);
    }

    static boolean stringContainsFlag(String input, final String[] flags)
    {
        if (input == null || flags == null) {
            return false;
        }

        input = input.toLowerCase();

        for (final String flag : flags) {
            final String lowerflag = flag.toLowerCase();

            if (input.contains(lowerflag)) {
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
