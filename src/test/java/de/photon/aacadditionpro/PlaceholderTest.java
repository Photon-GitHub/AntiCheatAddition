package de.photon.aacadditionpro;

import de.photon.aacadditionpro.util.execute.Placeholders;
import lombok.val;
import org.bukkit.ChatColor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class PlaceholderTest
{
    @Test
    void noPlaceholder()
    {
        val empty = "";
        val playerSingleton = Collections.singleton(Dummy.mockPlayer());
        Assertions.assertEquals(empty, Placeholders.replacePlaceholders(empty, playerSingleton));

        val string = "Some Spigot";
        Assertions.assertEquals(string, Placeholders.replacePlaceholders(string, playerSingleton));

        val color = ChatColor.translateAlternateColorCodes('&', "&4Some Spigot") + ChatColor.RESET;
        Assertions.assertEquals(color, Placeholders.replacePlaceholders(color, playerSingleton));
    }
}
