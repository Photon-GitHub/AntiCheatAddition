package de.photon.aacadditionpro.util.config

import org.apache.commons.lang.StringUtils
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException
import java.nio.file.Files

class ConfigurationRepresentation(private val configFile: File) {
    // Only needed for certain configs, therefore lazy.
    val yamlConfiguration: YamlConfiguration by lazy { YamlConfiguration.loadConfiguration(configFile) }
    private val requestedChanges: MutableMap<String, Any> = mutableMapOf()

    private fun searchForPath(lines: List<String>, path: String): Int {
        val pathParts = path.trim().split('.')
        var partIndex = 0
        var partDepth = 0
        var lineDepth: Int
        var trimmed: String

        for ((lineIndex, line) in lines.withIndex()) {
            lineDepth = StringUtil.depth(line)

            // The sub-part we search for does not exist.
            require(partDepth <= lineDepth) { "Path $path could not be found." }

            trimmed = line.trim()
            // New "deeper" subpart found?
            if (!isComment(trimmed) && trimmed.startsWith(pathParts[partIndex])) {
                partDepth = lineDepth
                // Whole path found.
                if (++partIndex == pathParts.size) return lineIndex
            }
        }
        throw IllegalArgumentException("Path $path could not be found (full iteration).")
    }

    @Synchronized
    fun requestValueChange(path: String, value: Any) {
        requestedChanges[path] = value
    }

    @Synchronized
    @Throws(IOException::class)
    fun save() {
        // Directly inject changes.
        if (requestedChanges.isEmpty()) return

        // Load the whole config.
        // Use LinkedList for fast mid-config tampering.
        val configLines = ArrayList(Files.readAllLines(configFile.toPath()))

        for ((path: String, value: Any) in requestedChanges) {
            val lineIndexOfKey = searchForPath(configLines, path)
            val originalLine = configLines[lineIndexOfKey]
            val affectedLines = linesOfKey(configLines, lineIndexOfKey)

            // We want to delete all lines after the initial one.
            deleteLines(configLines, lineIndexOfKey + 1, affectedLines - 1)

            // Add the removed ':' right back.
            val replacementLine = StringBuilder(StringUtils.substringBeforeLast(originalLine, ":")).append(':')

            // Set the new value.
            when (value) {
                // Simple sets
                is Boolean, is Byte, is Short, is Int, is Long, is Float, is Double -> replacementLine.append(' ').append(value)
                is String -> replacementLine.append(" \"").append(value).append('\"')
                // List logic to accommodate both empty lists as well as normal lists.
                is List<*> -> {
                    if (value.isEmpty()) {
                        replacementLine.append(" []")
                    } else {
                        val preString = StringUtils.leftPad("- ", StringUtil.depth(originalLine))
                        for (o in value) configLines.add(lineIndexOfKey + 1, preString + o)
                    }
                }
                // ConfigActions to allow special actions like the deletion of keys.
                is ConfigActions -> {
                    if (value === ConfigActions.DELETE_KEYS) {
                        replacementLine.append(" {}")
                    }
                }
            }
            configLines[lineIndexOfKey] = replacementLine.toString()
        }
        Files.write(configFile.toPath(), configLines)
    }

    enum class ConfigActions {
        DELETE_KEYS
    }

    private fun isComment(string: String?): Boolean {
        // == Because a '#' at a later point indicates some data before as leading whitespaces are removed.
        // Moreover, a negative value indicates that no comment char was found.
        return string == null || string.isEmpty() || string.trim().indexOf('#') == 0
    }

    private fun deleteLines(lines: MutableList<String>, startPosition: Int, lineCount: Int) {
        var i = lineCount
        while (i-- > 0) lines.removeAt(startPosition)
    }

    private fun linesOfKey(lines: List<String>, firstLineOfKey: Int): Int {
        // 1 as the first line is always there.
        var affectedLines = 1
        val depthOfKey = StringUtil.depth(lines[firstLineOfKey])

        // + 1 as the initial line should not be iterated over.
        for (line in lines.listIterator(firstLineOfKey + 1)) {
            if (StringUtil.depth(line) <= depthOfKey) break
            ++affectedLines
        }
        return affectedLines
    }
}