package de.photon.AACAdditionPro.util.verbose;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.events.ClientControlEvent;
import de.photon.AACAdditionPro.events.PlayerAdditionViolationEvent;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.commands.Placeholders;
import de.photon.AACAdditionPro.util.files.FileUtilities;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SuppressWarnings("MethodMayBeStatic")
public final class VerboseSender implements Listener
{
    private static boolean enabled;
    private static final String NON_COLORED_PRE_STRING = "[AACAdditionPro] ";
    private static final String PRE_STRING = ChatColor.DARK_RED + NON_COLORED_PRE_STRING + ChatColor.GRAY;

    @SuppressWarnings("unused")
    private static final VerboseSender instance = new VerboseSender();

    private VerboseSender()
    {
        setState(true);
        AACAdditionPro.getInstance().registerListener(this);
    }

    /**
     * This stores the options of how to log verbose messages
     * <p>
     * [0] stores whether AACAdditionPro should print verbose output in the console ({@link org.bukkit.command.ConsoleCommandSender} - Verbose)
     * <p>
     * [1] stores whether AACAdditionPro should save verbose output in a log file ({@link File} - Verbose)
     * <p>
     * [2] stores whether AACAdditionPro should print verbose output in the chat ({@link org.bukkit.entity.Player} - Verbose)
     */
    private static final boolean[] verboseOptions = {
            AACAdditionPro.getInstance().getConfig().getBoolean("Verbose.file"),
            AACAdditionPro.getInstance().getConfig().getBoolean("Verbose.console"),
            AACAdditionPro.getInstance().getConfig().getBoolean("Verbose.players")
    };

    public static void sendVerboseMessage(final String s)
    {
        sendVerboseMessage(s, false, false);
    }

    public static void sendVerboseMessage(final String s, final boolean force_console, final boolean error)
    {
        // Prevent errors on disable as of scheduling
        if (enabled) {
            final String logMessage = ChatColor.stripColor(s);

            if (verboseOptions[0]) {
                // Remove color codes
                log(logMessage);
            }

            if (verboseOptions[1] || force_console) {
                if (error) {
                    Bukkit.getLogger().severe(NON_COLORED_PRE_STRING + logMessage);
                } else {
                    Bukkit.getLogger().info(NON_COLORED_PRE_STRING + logMessage);
                }
            }

            if (verboseOptions[2]) {
                for (final User user : UserManager.getUsers()) {
                    if (user.verbose) {
                        Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> user.getPlayer().sendMessage(PRE_STRING + s));
                    }
                }
            }
        }
    }

    private static void log(final String message)
    {
        try {
            // Get the logfile that is in use currently or create a new one if needed.
            final File log_File = FileUtilities.saveFileInFolder(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".log", FileUtilities.AACADDITIONPRO_DATAFOLDER.getPath() + "/logs");

            // Add the beginning of the prefix
            final StringBuilder time = new StringBuilder("[");
            // Get the current time
            time.append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));

            // Add a 0 if it is too short
            while (time.length() < 12) {
                time.append("0");
            }

            // Add the rest of the prefix and the message
            time.append("] ");
            time.append(message);
            time.append("\n");

            // Log the message
            Files.write(log_File.toPath(), time.toString().getBytes(), StandardOpenOption.APPEND);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void on(final PlayerAdditionViolationEvent event)
    {
        sendVerboseMessage(Placeholders.applyPlaceholders(ChatColor.GOLD + "{player} " + ChatColor.GRAY + event.getMessage() + " | Vl: " + event.getVl() + " | TPS: {tps} | Ping: {ping}", event.getPlayer()));
    }

    @EventHandler
    public void on(final ClientControlEvent event)
    {
        sendVerboseMessage(Placeholders.applyPlaceholders(ChatColor.GOLD + "{player} " + ChatColor.GRAY + event.getMessage(), event.getPlayer()));
    }

    /**
     * Enables or disables the {@link VerboseSender}.
     *
     * @param flag whether to enable (true) or disable (false)
     */
    public static void setState(final boolean flag)
    {
        enabled = flag;
    }

    @EventHandler
    public void on(final PluginDisableEvent event)
    {
        if (event.getPlugin().getName().equals(AACAdditionPro.getInstance().getName())) {
            setState(false);
        }
    }
}
