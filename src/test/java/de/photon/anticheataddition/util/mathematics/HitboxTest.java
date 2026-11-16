package de.photon.anticheataddition.util.mathematics;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HitboxTest
{
    private static Hitbox.HitboxLocation at(Hitbox hitbox, double x, double y, double z)
    {
        return new Hitbox.HitboxLocation(hitbox, new Location(null, x, y, z));
    }

    private static void assertBlocks(Hitbox.HitboxLocation hitbox, Set<Integer> xs, Set<Integer> ys, Set<Integer> zs)
    {
        final var expected = new HashSet<Vector>();
        for (int x : xs) for (int y : ys) for (int z : zs) expected.add(new Vector(x, y, z));
        final var actual = hitbox.getPartiallyIncludedLocations().stream().map(Location::toVector).collect(Collectors.toSet());
        assertEquals(expected, actual);
    }

    @Test
    void positiveCoordinatesStayWithinTheirBlocks()
    {
        assertBlocks(at(Hitbox.PLAYER, 0.5, 64, 0.5), Set.of(0), Set.of(64, 65), Set.of(0));
    }

    @Test
    void zeroCrossingsIncludeBothSides()
    {
        assertBlocks(at(Hitbox.PLAYER, -0.2, 64, -0.2), Set.of(-1, 0), Set.of(64, 65), Set.of(-1, 0));
    }

    @Test
    void negativeCoordinatesUseTheOccupiedBlock()
    {
        assertBlocks(at(Hitbox.PLAYER, -1.5, 64, -1.5), Set.of(-2), Set.of(64, 65), Set.of(-2));
    }

    @Test
    void negativeHeightIncludesTheBlockBelowZero()
    {
        assertBlocks(at(Hitbox.PLAYER, 0.5, -0.2, 0.5), Set.of(0), Set.of(-1, 0, 1), Set.of(0));
    }

    @Test
    void exactHorizontalBoundariesRemainInclusive()
    {
        assertBlocks(at(Hitbox.ESP_PLAYER, 0.5, 64, 0.5), Set.of(0, 1), Set.of(64, 65), Set.of(0, 1));
        assertBlocks(at(Hitbox.ESP_PLAYER, -0.5, -2, -0.5), Set.of(-1, 0), Set.of(-2, -1), Set.of(-1, 0));
    }

    @Test
    void exactUpperHeightRemainsInclusive()
    {
        assertBlocks(at(Hitbox.PLAYER, 0.5, 0.2, 0.5), Set.of(0), Set.of(0, 1, 2), Set.of(0));
    }

    @Test
    void espKeepsCornersAcrossZero()
    {
        assertEquals(8, at(Hitbox.ESP_PLAYER, 0.2, 64, 0.2).getEspLocations().length);
        assertEquals(8, at(Hitbox.ESP_PLAYER, -0.2, 64, -0.2).getEspLocations().length);
    }

    @Test
    void espCullsAxesContainedWithinOneBlock()
    {
        assertEquals(2, at(Hitbox.PLAYER, 0.5, 64, 0.5).getEspLocations().length);
        assertEquals(2, at(Hitbox.PLAYER, -1.5, 64, -1.5).getEspLocations().length);

        final var cullX = at(Hitbox.PLAYER, -1.5, 64, -0.2).getEspLocations();
        assertEquals(4, cullX.length);
        assertTrue(Arrays.stream(cullX).allMatch(location -> location.getX() == -1.5));

        final var cullZ = at(Hitbox.PLAYER, -0.2, 64, -1.5).getEspLocations();
        assertEquals(4, cullZ.length);
        assertTrue(Arrays.stream(cullZ).allMatch(location -> location.getZ() == -1.5));
    }
}
