package de.photon.AACAdditionPro.util;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.events.ClientControlEvent;
import de.photon.AACAdditionPro.events.PlayerAdditionViolationEvent;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.commands.Placeholders;
import de.photon.AACAdditionPro.util.files.FileUtilities;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class VerboseSender implements Listener
{
    @Getter
    private static final VerboseSender instance = new VerboseSender();

    // Message constants
    private static final String NON_COLORED_PRE_STRING = "[AACAdditionPro] ";
    private static final String PRE_STRING = ChatColor.DARK_RED + NON_COLORED_PRE_STRING + ChatColor.GRAY;
    private static final String EVENT_PRE_STRING = ChatColor.GOLD + "{player} " + ChatColor.GRAY;

    @Setter
    private boolean allowedToRegisterTasks;

    // The File the verbose messages are written to.
    private File logFile = null;
    // Set to an impossible day of the year to make sure the logFile will be initialized.
    private int currentDayOfYear = -1;

    private boolean writeToFile = AACAdditionPro.getInstance().getConfig().getBoolean("Verbose.file");
    private boolean writeToConsole = AACAdditionPro.getInstance().getConfig().getBoolean("Verbose.console");
    private boolean writeToPlayers = AACAdditionPro.getInstance().getConfig().getBoolean("Verbose.players");

    private VerboseSender()
    {
        allowedToRegisterTasks = true;
        AACAdditionPro.getInstance().registerListener(this);
    }

    /**
     * Sets off a standard verbose message (no console forcing and not flagged as an error).
     *
     * @param s the message that will be sent
     */
    public void sendVerboseMessage(final String s)
    {
        sendVerboseMessage(s, false, false);
    }

    /**
     * This sets off a verbose message.
     *
     * @param s             the message that will be sent
     * @param force_console whether the verbose message should appear in the console even when verbose for console is deactivated.
     * @param error         whether the message should be marked as an error
     */
    public void sendVerboseMessage(final String s, final boolean force_console, final boolean error)
    {
        // Prevent errors on disable as of scheduling
        final String logMessage = ChatColor.stripColor(s);

        if (writeToFile)
        {
            // Remove color codes
            this.log(logMessage);
        }

        if (writeToConsole || force_console)
        {
            if (error)
            {
                Bukkit.getLogger().severe(NON_COLORED_PRE_STRING + logMessage);
            }
            else
            {
                Bukkit.getLogger().info(NON_COLORED_PRE_STRING + logMessage);
            }
        }

        // Prevent error on disable
        if (allowedToRegisterTasks && writeToPlayers)
        {
            Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> {
                for (User user : UserManager.getVerboseUsers())
                {
                    user.getPlayer().sendMessage(PRE_STRING + s);
                }
            });
        }
    }

    private void log(final String message)
    {
        try
        {
            // Get the logfile that is in use currently or create a new one if needed.
            final LocalDateTime now = LocalDateTime.now();

            // Doesn't need to check for logFile == null as the currentDayOfYear will be -1 in the beginning.
            if (currentDayOfYear != now.getDayOfYear() || !logFile.exists())
            {
                currentDayOfYear = now.getDayOfYear();
                logFile = FileUtilities.saveFileInFolder("logs/" + now.format(DateTimeFormatter.ISO_LOCAL_DATE) + ".log");
            }

            // Reserve the required builder size.
            // Time length is always 12, together with 2 brackets and one space this will result in 15.
            final StringBuilder verboseMessage = new StringBuilder(15 + message.length());
            // Add the beginning of the PREFIX
            verboseMessage.append('[');
            // Get the current time
            verboseMessage.append(now.format(DateTimeFormatter.ISO_LOCAL_TIME));

            // Add a 0 if it is too short
            // Technically only 12, but we already appended the "[", thus one more.
            while (verboseMessage.length() < 13)
            {
                verboseMessage.append('0');
            }

            // Add the rest of the PREFIX and the message
            verboseMessage.append("] ");
            verboseMessage.append(message);
            verboseMessage.append('\n');

            // Log the message
            Files.write(logFile.toPath(), verboseMessage.toString().getBytes(), StandardOpenOption.APPEND);
        } catch (final IOException e)
        {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void on(final PlayerAdditionViolationEvent event)
    {
        this.sendVerboseMessage(Placeholders.applyPlaceholders(EVENT_PRE_STRING + event.getMessage() + " | Vl: " + event.getVl() + " | TPS: {tps} | Ping: {ping}", event.getPlayer(), null));
    }

    @EventHandler
    public void on(final ClientControlEvent event)
    {
        this.sendVerboseMessage(Placeholders.applyPlaceholders(EVENT_PRE_STRING + event.getMessage(), event.getPlayer(), null));
    }
}
