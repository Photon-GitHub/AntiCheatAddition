package de.photon.aacadditionpro.util.messaging

import de.photon.aacadditionproold.AACAdditionPro
import de.photon.aacadditionproold.util.files.FileUtil
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.Level

class LogFile(now: LocalDateTime) {
    private val backingFile: File?
    private val dayOfTheYear: Int = now.dayOfYear

    fun write(logMessage: String, now: LocalDateTime) {
        if (backingFile != null) {
            // Reserve the required builder size.
            // Time length is always 12, together with 2 brackets and one space this will result in 15.
            val verboseMessage = StringBuilder(15 + logMessage.length)
            // Add the beginning of the PREFIX and format the time.
            verboseMessage.append('[').append(now.format(DateTimeFormatter.ISO_LOCAL_TIME))

            // Add a 0 if it is too short
            // Technically only 12, but we already appended the "[", thus one more.
            while (verboseMessage.length < 13) verboseMessage.append('0')

            // Add the rest of the PREFIX and the message
            verboseMessage.append(']').append(' ').append(logMessage).append('\n')

            try {
                // Log the message
                Files.write(backingFile.toPath(), verboseMessage.toString().toByteArray(), StandardOpenOption.APPEND)
            } catch (e: IOException) {
                AACAdditionPro.getInstance().logger.log(Level.SEVERE, "Something went wrong while trying to write to the log file", e)
            }
        }
    }

    fun isValid(now: LocalDateTime): Boolean {
        return dayOfTheYear == now.dayOfYear && backingFile != null && backingFile.exists()
    }

    init {
        var tempFile: File? = null

        try {
            tempFile = FileUtil.createFile(File(AACAdditionPro.getInstance().dataFolder.path + "/logs/" + now.format(DateTimeFormatter.ISO_LOCAL_DATE) + ".log"))
        } catch (e: IOException) {
            AACAdditionPro.getInstance().logger.log(Level.SEVERE, "Something went wrong while trying to create the log file", e)
        }

        backingFile = tempFile
    }
}