package de.photon.anticheataddition.util;

import de.photon.anticheataddition.Dummy;
import de.photon.anticheataddition.util.execute.Placeholders;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class PlaceholderTest
{
    @BeforeAll
    static void setup()
    {
        Dummy.mockAntiCheatAddition();
    }

    @Test
    void returnsOriginalStringWhenNoPlaceholderIsPresent()
    {
        final var empty = "";
        final var original = "Some Spigot";
        final var player = Mockito.mock(Player.class);

        assertSame(empty, Placeholders.replacePlaceholders(empty, player));
        assertSame(empty, Placeholders.replacePlaceholdersSafely(empty));
        assertSame(original, Placeholders.replacePlaceholders(original, player));
        assertSame(original, Placeholders.replacePlaceholdersSafely(original));
    }

    @Test
    void safeReplacementOnlyHandlesGlobalPlaceholders()
    {
        assertEquals("{player} {ping} {world}", Placeholders.replacePlaceholdersSafely("{player} {ping} {world}"));
        assertTrue(Placeholders.replacePlaceholdersSafely("{date}").matches("\\d{4}-\\d{2}-\\d{2}"));
        assertTrue(Placeholders.replacePlaceholdersSafely("{time}").matches("\\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void playerReplacementRequiresPlayer()
    {
        assertThrows(NullPointerException.class, () -> Placeholders.replacePlaceholders("{player}", null));
    }

    @Test
    void playerPlaceholdersAreReplaced()
    {
        final var player = Mockito.mock(Player.class);
        Mockito.when(player.getName()).thenReturn("Photon");
        Mockito.when(player.getPing()).thenReturn(47);

        assertEquals("Photon/47", Placeholders.replacePlaceholders("{player}/{ping}", player));
    }

    @Test
    void worldPlaceholderIsReplaced()
    {
        final var world = Mockito.mock(World.class);
        Mockito.when(world.getName()).thenReturn("world_nether");

        final var player = Mockito.mock(Player.class);
        Mockito.when(player.getWorld()).thenReturn(world);

        assertEquals("world_nether", Placeholders.replacePlaceholders("{world}", player));
    }

    @Test
    void unknownPlaceholdersStayLiteral()
    {
        final var player = Mockito.mock(Player.class);

        assertEquals("Hello {unknown}", Placeholders.replacePlaceholdersSafely("Hello {unknown}"));
        assertEquals("{player_name}", Placeholders.replacePlaceholders("{player_name}", player));
    }

    @Test
    void adjacentAndRepeatedPlaceholdersAreAllReplaced()
    {
        final var player = Mockito.mock(Player.class);
        Mockito.when(player.getName()).thenReturn("Photon");

        assertEquals("PhotonPhoton/Photon", Placeholders.replacePlaceholders("{player}{player}/{player}", player));
    }

    @Test
    void literalBraceCasesStayLiteral()
    {
        final var player = Mockito.mock(Player.class);
        Mockito.when(player.getName()).thenReturn("Photon");

        assertEquals("Hello {player", Placeholders.replacePlaceholders("Hello {player", player));
        assertEquals("Photon {world", Placeholders.replacePlaceholders("{player} {world", player));
        assertEquals("Hello {}", Placeholders.replacePlaceholdersSafely("Hello {}"));
        assertEquals("Hello }", Placeholders.replacePlaceholdersSafely("Hello }"));
    }
}
