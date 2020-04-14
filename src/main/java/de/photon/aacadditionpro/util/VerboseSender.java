package de.photon.aacadditionpro.util;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.events.ClientControlEvent;
import de.photon.aacadditionpro.events.PlayerAdditionViolationEvent;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.util.commands.Placeholders;
import de.photon.aacadditionpro.util.files.FileUtil;
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
import java.util.logging.Level;

public final class VerboseSender implements Listener
{
    @Getter
    private static final VerboseSender instance;
    private static final String PRE_STRING = ChatColor.DARK_RED + "[AACAdditionPro] " + ChatColor.GRAY;
    private static final String EVENT_PRE_STRING = ChatColor.GOLD + "{player} " + ChatColor.GRAY;

    static {
        instance = new VerboseSender();
        AACAdditionPro.getInstance().registerListener(instance);
    }

    private final boolean writeToFile = AACAdditionPro.getInstance().getConfig().getBoolean("Verbose.file");
    private final boolean writeToConsole = AACAdditionPro.getInstance().getConfig().getBoolean("Verbose.console");
    private final boolean writeToPlayers = AACAdditionPro.getInstance().getConfig().getBoolean("Verbose.players");
    @Setter
    private boolean allowedToRegisterTasks;
    // The File the verbose messages are written to.
    private File logFile = null;
    // Set to an impossible day of the year to make sure the logFile will be initialized.
    private int currentDayOfYear = -1;

    private VerboseSender()
    {
        allowedToRegisterTasks = true;
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
        // Remove color codes
        final String logMessage = ChatColor.stripColor(s);

        if (writeToFile) {
            try {
                // Get the logfile that is in use currently or create a new one if needed.
                final LocalDateTime now = LocalDateTime.now();

                // Doesn't need to check for logFile == null as the currentDayOfYear will be -1 in the beginning.
                if (currentDayOfYear != now.getDayOfYear() || !logFile.exists()) {
                    currentDayOfYear = now.getDayOfYear();
                    logFile = FileUtil.createFile(new File(AACAdditionPro.getInstance().getDataFolder().getPath() + "/logs/" + now.format(DateTimeFormatter.ISO_LOCAL_DATE) + ".log"));
                }

                // Reserve the required builder size.
                // Time length is always 12, together with 2 brackets and one space this will result in 15.
                final StringBuilder verboseMessage = new StringBuilder(15 + logMessage.length());
                // Add the beginning of the PREFIX
                verboseMessage.append('[');
                // Get the current time
                verboseMessage.append(now.format(DateTimeFormatter.ISO_LOCAL_TIME));

                // Add a 0 if it is too short
                // Technically only 12, but we already appended the "[", thus one more.
                while (verboseMessage.length() < 13) {
                    verboseMessage.append('0');
                }

                // Add the rest of the PREFIX and the message
                verboseMessage.append("] ");
                verboseMessage.append(logMessage);
                verboseMessage.append('\n');

                // Log the message
                Files.write(logFile.toPath(), verboseMessage.toString().getBytes(), StandardOpenOption.APPEND);
            } catch (final IOException e) {
                AACAdditionPro.getInstance().getLogger().log(Level.SEVERE, "Something went wrong while trying to write to the log file.", e);
            }
        }

        if (writeToConsole || force_console) {
            AACAdditionPro.getInstance().getLogger().log(error ?
                                                         Level.SEVERE :
                                                         Level.INFO, logMessage);
        }

        // Prevent errors on disable as of scheduling
        if (allowedToRegisterTasks && writeToPlayers) {
            Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> {
                for (User user : UserManager.getVerboseUsers()) {
                    user.getPlayer().sendMessage(PRE_STRING + s);
                }
            });
        }
    }

    @EventHandler
    public void onAdditionViolation(final PlayerAdditionViolationEvent event)
    {
        this.sendVerboseMessage(Placeholders.replacePlaceholders(EVENT_PRE_STRING + event.getMessage() + " | Vl: " + event.getVl() + " | TPS: {tps} | Ping: {ping}", event.getPlayer()));
    }

    @EventHandler
    public void onClientControl(final ClientControlEvent event)
    {
        this.sendVerboseMessage(Placeholders.replacePlaceholders(EVENT_PRE_STRING + event.getMessage(), event.getPlayer()));
    }
}
