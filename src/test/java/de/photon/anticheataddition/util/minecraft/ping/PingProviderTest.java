package de.photon.anticheataddition.util.minecraft.ping;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class PingProviderTest {

    @Test
    void atMostMaxPingCases() {
        int[] pings = {50, 100, 150};
        boolean[] expected = {true, true, false};
        int max = 100;
        Player player = mock(Player.class);
        for (int i = 0; i < pings.length; i++) {
            int ping = pings[i];
            PingProvider provider = p -> ping;
            assertEquals(expected[i], provider.atMostMaxPing(player, max), "ping=" + ping);
        }
    }

    @Test
    void negativeMaxPingAlwaysTrue() {
        int[] pings = {0, 100, 200};
        Player player = mock(Player.class);
        for (int ping : pings) {
            PingProvider provider = p -> ping;
            assertTrue(provider.atMostMaxPing(player, -1));
        }
    }
}
