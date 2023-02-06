package de.photon.anticheataddition.util;

import de.photon.anticheataddition.Dummy;
import de.photon.anticheataddition.util.execute.Placeholders;
import org.bukkit.ChatColor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PlaceholderTest
{
    @Test
    void noPlaceholder()
    {
        final var empty = "";
        final var player = Dummy.mockPlayer();
        Assertions.assertEquals(empty, Placeholders.replacePlaceholders(empty, player));

        final var string = "Some Spigot";
        Assertions.assertEquals(string, Placeholders.replacePlaceholders(string, player));

        final var color = ChatColor.translateAlternateColorCodes('&', "&4Some Spigot") + ChatColor.RESET;
        Assertions.assertEquals(color, Placeholders.replacePlaceholders(color, player));
    }
}
