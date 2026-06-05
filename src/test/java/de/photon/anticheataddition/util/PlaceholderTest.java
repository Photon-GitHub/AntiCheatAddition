package de.photon.anticheataddition.util;

import de.photon.anticheataddition.Dummy;
import de.photon.anticheataddition.util.execute.Placeholders;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PlaceholderTest {
    @BeforeAll
    static void setup()
    {
        Dummy.mockAntiCheatAddition();
    }

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

    @Test
    void simplePlaceholdersWithoutPlayer()
    {
        Assertions.assertEquals("Some Spigot", Placeholders.replacePlaceholdersSafely("Some Spigot"));
        Assertions.assertEquals("{player} {ping} {world}", Placeholders.replacePlaceholdersSafely("{player} {ping} {world}"));
        Assertions.assertTrue(Placeholders.replacePlaceholdersSafely("{date}").matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    void normalPlaceholdersRequirePlayer()
    {
        Assertions.assertThrows(NullPointerException.class, () -> Placeholders.replacePlaceholders("{player}", null));
    }

    @Test
    void playerPlaceholders()
    {
        final var player = Dummy.mockPlayer();
        Assertions.assertEquals(player.getName(), Placeholders.replacePlaceholders("{player}", player));
        Assertions.assertEquals("0", Placeholders.replacePlaceholders("{ping}", player));
    }

    @Test
    void playerPlaceholderIsLimited()
    {
        final var player = Mockito.mock(Player.class);
        Mockito.when(player.getName()).thenReturn("123456789012345678901234567890extra");

        Assertions.assertEquals("123456789012345678901234567890", Placeholders.replacePlaceholders("{player}", player));
    }

    @Test
    void worldPlaceholder()
    {
        final var world = Mockito.mock(World.class);
        Mockito.when(world.getName()).thenReturn("world_nether");

        final var player = Mockito.mock(Player.class);
        Mockito.when(player.getWorld()).thenReturn(world);

        Assertions.assertEquals("world_nether", Placeholders.replacePlaceholders("{world}", player));
    }

    @Test
    void unknownPlaceholdersStayLiteral()
    {
        Assertions.assertEquals("Hello {unknown}", Placeholders.replacePlaceholdersSafely("Hello {unknown}"));
        Assertions.assertEquals("{player_name}", Placeholders.replacePlaceholders("{player_name}", Dummy.mockPlayer()));
    }

    @Test
    void repeatedPlaceholdersAreAllReplaced()
    {
        final var player = Dummy.mockPlayer();
        final var expected = player.getName() + "/" + player.getName();

        Assertions.assertEquals(expected, Placeholders.replacePlaceholders("{player}/{player}", player));
    }

    @Test
    void simpleDateAndTimePlaceholders()
    {
        Assertions.assertTrue(Placeholders.replacePlaceholdersSafely("{date}").matches("\\d{4}-\\d{2}-\\d{2}"));
        Assertions.assertTrue(Placeholders.replacePlaceholdersSafely("{time}").matches("\\d{2}:\\d{2}:\\d{2}"));
    }
}
