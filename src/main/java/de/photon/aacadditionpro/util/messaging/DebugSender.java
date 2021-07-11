package de.photon.aacadditionpro.util.messaging;

import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.events.SentinelEvent;
import de.photon.aacadditionpro.events.ViolationEvent;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.execute.Placeholders;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.val;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.time.LocalDateTime;
import java.util.logging.Level;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DebugSender implements Listener
{
    @Getter private static final DebugSender instance;
    private static final String SENTINEL_PRE_STRING = (ChatColor.WHITE + "{player} " + ChatColor.GRAY) + "Sentinel detection: ";
    private static final String VIOLATION_PRE_STRING = (ChatColor.WHITE + "{player} " + ChatColor.GRAY) + "module detection: ";

    static {
        instance = new DebugSender();
        AACAdditionPro.getInstance().registerListener(instance);
    }

    private final boolean writeToFile = AACAdditionPro.getInstance().getConfig().getBoolean("Debug.file");
    private final boolean writeToConsole = AACAdditionPro.getInstance().getConfig().getBoolean("Debug.console");
    private final boolean writeToPlayers = AACAdditionPro.getInstance().getConfig().getBoolean("Debug.players");

    @Setter private volatile boolean allowedToRegisterTasks = true;
    // The File the debug messages are written to.
    private LogFile logFile = new LogFile(LocalDateTime.now());

    /**
     * Sets off a standard debug message (no console forcing and not flagged as an error).
     *
     * @param s the message that will be sent
     */
    public void sendDebug(final String s)
    {
        sendDebug(s, false, false);
    }

    /**
     * This sets off a debug message.
     *
     * @param s             the message that will be sent
     * @param force_console whether the debug message should appear in the console even when debug for console is deactivated.
     * @param error         whether the message should be marked as an error
     */
    public void sendDebug(final String s, final boolean force_console, final boolean error)
    {
        // Remove color codes
        val logMessage = ChatColor.stripColor(s);

        if (writeToFile) {
            // Get the logfile that is in use currently or create a new one if needed.
            val now = LocalDateTime.now();
            if (!this.logFile.isValid(now)) this.logFile = new LogFile(now);
            this.logFile.write(logMessage, now);
        }

        if (writeToConsole || force_console) {
            AACAdditionPro.getInstance().getLogger().log(error ?
                                                         Level.SEVERE :
                                                         Level.INFO, logMessage);
        }

        // Prevent errors on disable as of scheduling
        if (allowedToRegisterTasks && writeToPlayers) {
            ChatMessage.sendSyncMessage(User.getPlayers(User.getDebugUsers()), s);
        }
    }

    @EventHandler
    public void onAdditionViolation(final ViolationEvent event)
    {
        this.sendDebug(Placeholders.replacePlaceholders(VIOLATION_PRE_STRING + event.getModuleId() + " | added vl: " + event.getVl() + " | TPS: {tps} | Ping: {ping}", ImmutableSet.of(event.getPlayer())));
    }

    @EventHandler
    public void onClientControl(final SentinelEvent event)
    {
        this.sendDebug(Placeholders.replacePlaceholders(SENTINEL_PRE_STRING + event.getModuleId(), ImmutableSet.of(event.getPlayer())));
    }
}
