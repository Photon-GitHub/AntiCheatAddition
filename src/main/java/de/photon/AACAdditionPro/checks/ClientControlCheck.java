package de.photon.AACAdditionPro.checks;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.events.ClientControlEvent;
import de.photon.AACAdditionPro.events.PlayerAdditionViolationCommandEvent;
import de.photon.AACAdditionPro.util.commands.Placeholders;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
    default void executeThresholds(final Player player)
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
            for (final String rawCommand : getCommandsOnDetection()) {
                final String realCommand = Placeholders.applyPlaceholders(rawCommand, player);
                //Sync command execution
                Bukkit.getScheduler().scheduleSyncDelayedTask(
                        AACAdditionPro.getInstance(), () ->
                        {
                            //Try catch to prevent console errors if a command couldn't be executed, e.g. if the player has left.
                            try {
                                final PlayerAdditionViolationCommandEvent playerAdditionViolationCommandEvent = new PlayerAdditionViolationCommandEvent(player, realCommand, this.getAdditionHackType());
                                AACAdditionPro.getInstance().getServer().getPluginManager().callEvent(playerAdditionViolationCommandEvent);

                                if (!playerAdditionViolationCommandEvent.isCancelled()) {
                                    AACAdditionPro.getInstance().getServer().dispatchCommand(AACAdditionPro.getInstance().getServer().getConsoleSender(), playerAdditionViolationCommandEvent.getCommand());
                                    VerboseSender.sendVerboseMessage(ChatColor.GOLD + " Punisher: Executed command /" + playerAdditionViolationCommandEvent.getCommand());
                                }
                            } catch (final Exception e) {
                                VerboseSender.sendVerboseMessage("Could not execute command /" + realCommand + ". If you change the command in the event please take a look at it, the command does not represent the event-command.", true, true);
                            }
                        });
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
}
