package de.photon.aacadditionpro.util.messaging

import de.photon.aacadditionpro.user.User
import de.photon.aacadditionproold.AACAdditionPro
import de.photon.aacadditionproold.events.ClientControlEvent
import de.photon.aacadditionproold.events.PlayerAdditionViolationEvent
import de.photon.aacadditionproold.util.commands.Placeholders
import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.time.LocalDateTime
import java.util.logging.Level

object DebugSender : Listener {
    init {
        AACAdditionPro.getInstance().registerListener(this)
    }

    @JvmStatic
    private val EVENT_PRE_STRING = ChatColor.WHITE.toString() + "{player} " + ChatColor.GRAY

    private val writeToFile = AACAdditionPro.getInstance().config.getBoolean("Debug.file")
    private val writeToConsole = AACAdditionPro.getInstance().config.getBoolean("Debug.console")
    private val writeToPlayers = AACAdditionPro.getInstance().config.getBoolean("Debug.players")

    @Volatile
    private var allowedToRegisterTasks = true

    // The File the verbose messages are written to.
    private var logFile: LogFile = LogFile(LocalDateTime.now())

    /**
     * This sets off a verbose message.
     *
     * @param s             the message that will be sent
     * @param force_console whether the verbose message should appear in the console even when verbose for console is deactivated.
     * @param error         whether the message should be marked as an error
     */
    @JvmOverloads
    fun sendDebug(s: String?, force_console: Boolean = false, error: Boolean = false) {
        // Remove color codes
        val logMessage: String = ChatColor.stripColor(s).toString()

        if (writeToFile) {
            // Get the logfile that is in use currently or create a new one if needed.
            val now = LocalDateTime.now()
            if (!logFile.isValid(now)) logFile = LogFile(now)
            logFile.write(logMessage, now)
        }

        if (writeToConsole || force_console) AACAdditionPro.getInstance().logger.log(if (error) Level.SEVERE else Level.INFO, logMessage)

        // Prevent errors on disable as of scheduling
        if (allowedToRegisterTasks && writeToPlayers) ChatMessage.sendSyncMessage(User.getDebugUsers(), s)
    }

    @EventHandler
    fun onAdditionViolation(event: PlayerAdditionViolationEvent) {
        sendDebug(Placeholders.replacePlaceholders(EVENT_PRE_STRING + event.message + " | Vl: " + event.vl + " | TPS: {tps} | Ping: {ping}", event.player))
    }

    @EventHandler
    fun onClientControl(event: ClientControlEvent) {
        sendDebug(Placeholders.replacePlaceholders(EVENT_PRE_STRING + event.message, event.player))
    }

    fun setAllowedToRegisterTasks(allowedToRegisterTasks: Boolean) {
        this.allowedToRegisterTasks = allowedToRegisterTasks
    }
}