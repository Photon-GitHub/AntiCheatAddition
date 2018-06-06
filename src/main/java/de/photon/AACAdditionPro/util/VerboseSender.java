package de.photon.AACAdditionPro.util;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.events.ClientControlEvent;
import de.photon.AACAdditionPro.events.PlayerAdditionViolationEvent;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.commands.Placeholders;
import de.photon.AACAdditionPro.util.files.FileUtilities;
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
    @Setter
    private static boolean allowedToRegisterTasks;

    // Used for sendVerboseMessage
    private static final String NON_COLORED_PRE_STRING = "[AACAdditionPro] ";
    private static final String PRE_STRING = ChatColor.DARK_RED + NON_COLORED_PRE_STRING + ChatColor.GRAY;

    // Used for the Events-Verbose
    private static final String EVENT_PRE_STRING = ChatColor.GOLD + "{player} " + ChatColor.GRAY;

    @SuppressWarnings("unused")
    private static final VerboseSender instance = new VerboseSender();

    private VerboseSender()
    {
        allowedToRegisterTasks = true;
        AACAdditionPro.getInstance().registerListener(this);
    }

    /**
     * This stores the options of how to log verbose messages
     * <p>
     * [0] stores whether AACAdditionPro should print verbose output in the console ({@link org.bukkit.command.ConsoleCommandSender} - Verbose)
     * <p>
     * [1] stores whether AACAdditionPro should saveToFile verbose output in a log file ({@link File} - Verbose)
     * <p>
     * [2] stores whether AACAdditionPro should print verbose output in the chat ({@link org.bukkit.entity.Player} - Verbose)
     */
    private static final boolean[] verboseOptions = {
            AACAdditionPro.getInstance().getConfig().getBoolean("Verbose.file"),
            AACAdditionPro.getInstance().getConfig().getBoolean("Verbose.console"),
            AACAdditionPro.getInstance().getConfig().getBoolean("Verbose.players")
    };


    /**
     * Sets off a standard verbose message (no console forcing and not flagged as an error).
     *
     * @param s the message that will be sent
     */
    public static void sendVerboseMessage(final String s)
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
    public static void sendVerboseMessage(final String s, final boolean force_console, final boolean error)
    {
        // Prevent errors on disable as of scheduling
        final String logMessage = ChatColor.stripColor(s);

        if (verboseOptions[0])
        {
            // Remove color codes
            log(logMessage);
        }

        if (verboseOptions[1] || force_console)
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
        if (allowedToRegisterTasks && verboseOptions[2])
        {
            for (final User user : UserManager.getUsersUnwrapped())
            {
                if (user.verbose)
                {
                    Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> user.getPlayer().sendMessage(PRE_STRING + s));
                }
            }
        }
    }

    private static void log(final String message)
    {
        try
        {
            // Get the logfile that is in use currently or create a new one if needed.
            final File log_File = FileUtilities.saveFileInFolder("logs/" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".log");

            // Reserve the required builder size.
            // Time length is always 12, together with 2 brackets and one space this will result in 15.
            final StringBuilder time = new StringBuilder(15 + message.length());
            // Add the beginning of the PREFIX
            time.append('[');
            // Get the current time
            time.append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));

            // Add a 0 if it is too short
            // Technically only 12, but we already appended the "[", thus one more.
            while (time.length() < 13)
            {
                time.append('0');
            }

            // Add the rest of the PREFIX and the message
            time.append("] ");
            time.append(message);
            time.append('\n');

            // Log the message
            Files.write(log_File.toPath(), time.toString().getBytes(), StandardOpenOption.APPEND);
        } catch (final IOException e)
        {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void on(final PlayerAdditionViolationEvent event)
    {
        sendVerboseMessage(Placeholders.applyPlaceholders(EVENT_PRE_STRING + event.getMessage() + " | Vl: " + event.getVl() + " | TPS: {tps} | Ping: {ping}", event.getPlayer(), null));
    }

    @EventHandler
    public void on(final ClientControlEvent event)
    {
        sendVerboseMessage(Placeholders.applyPlaceholders(EVENT_PRE_STRING + event.getMessage(), event.getPlayer(), null));
    }
}
