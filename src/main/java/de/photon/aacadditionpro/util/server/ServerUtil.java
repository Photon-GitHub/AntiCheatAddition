package de.photon.aacadditionpro.util.server;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.exception.UnknownMinecraftException;
import de.photon.aacadditionpro.util.datastructure.buffer.RingBuffer;
import de.photon.aacadditionpro.util.reflection.ClassReflect;
import de.photon.aacadditionpro.util.reflection.Reflect;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;

/**
 * This util provides methods to get information from the server that is usually hidden.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ServerUtil
{
    private static final ClassReflect CRAFTPLAYER_CLASS_REFLECT = Reflect.fromOBC("entity.CraftPlayer");
    private static final TPS TPS = new TPS(AACAdditionPro.getInstance());

    // Use ConditionalCommands logic.
    /*MIT License
    Copyright (c) 2018 Vincent Tang

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.*/

    /**
     * Reflects the real ping of a {@link Player} from the CraftPlayer class.
     *
     * @param player the {@link Player} whose ping should be returned.
     *
     * @return the ping of the player or 0 if an exception is thrown.
     */
    public static int getPing(final Player player)
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC18:
            case MC19:
            case MC110:
            case MC111:
            case MC112:
            case MC113:
            case MC114:
            case MC115:
                val craftPlayer = CRAFTPLAYER_CLASS_REFLECT.method("getHandle").invoke(player);
                try {
                    return craftPlayer.getClass().getDeclaredField("ping").getInt(craftPlayer);
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    AACAdditionPro.getInstance().getLogger().log(Level.WARNING, "Failed to retrieve ping of a player: ", e);
                    return 0;
                }
            case MC116:
                return player.getPing();
            default:
                throw new UnknownMinecraftException();
        }
    }

    /**
     * Gets the current TPS of the server.
     */
    public static double getTPS()
    {
        return TPS.getCurrentTPS();
    }

    private static class TPS extends BukkitRunnable
    {
        private static final int RESOLUTION = 40;
        private final RingBuffer<Long> tickIntervals;
        private long lastTick;

        private TPS(final Plugin plugin)
        {
            this.lastTick = System.currentTimeMillis();
            this.tickIntervals = new RingBuffer<>(RESOLUTION);
            // Init the RingBuffer with 50 millis for 20 TPS.
            for (int i = 0; i < RESOLUTION; ++i) this.tickIntervals.add(50L);
            this.runTaskTimer(plugin, 1L, 1L);
        }

        double getCurrentTPS()
        {
            val delta = this.getDelta();
            if (delta == 0) return 20.0;

            // 1000 milliseconds per second, delta is also milliseconds -> ticks. As the maximum of ticks is 20, allow no value above 20.
            return Math.min(1000.0 / delta, 20.0);
        }

        public void run()
        {
            val curr = System.currentTimeMillis();
            val delta = curr - this.lastTick;
            this.lastTick = curr;
            this.tickIntervals.add(delta);
        }

        private double getDelta()
        {
            long base = 0;
            for (final long delta : this.tickIntervals) base += delta;
            return (double) (base / RESOLUTION);
        }
    }
}
