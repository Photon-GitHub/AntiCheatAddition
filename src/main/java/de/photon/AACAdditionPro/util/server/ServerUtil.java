package de.photon.AACAdditionPro.util.server;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.util.reflection.ClassReflect;
import de.photon.AACAdditionPro.util.reflection.Reflect;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;

/**
 * This util provides methods to get information from the server that is usually hidden.
 */
public final class ServerUtil
{
    private static final ClassReflect CRAFTPLAYER_CLASS_REFLECT = Reflect.fromOBC("entity.CraftPlayer");

    /**
     * Reflects the real ping of a {@link Player} from the CraftPlayer class.
     *
     * @param player the {@link Player} whose ping should be returned.
     *
     * @return the ping of the player or 0 if an exception is thrown.
     */
    public static int getPing(final Player player)
    {
        final Object craftPlayer = CRAFTPLAYER_CLASS_REFLECT.method("getHandle").invoke(player);
        try {
            return craftPlayer.getClass().getDeclaredField("ping").getInt(craftPlayer);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
            return 0;
        }
    }

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

    private static final TPS TPS = new TPS(AACAdditionPro.getInstance());

    /**
     * Gets the current TPS of the server.
     */
    public static double getTPS()
    {
        return TPS.getCurrentTPS();
    }

    private static class TPS extends BukkitRunnable
    {
        private final int resolution;
        private long lastTick;
        private final Deque<Long> tickIntervals;

        private TPS(final Plugin plugin)
        {
            this.resolution = 40;
            this.lastTick = System.currentTimeMillis();
            this.tickIntervals = new ArrayDeque<>(Collections.nCopies(this.resolution, 50L));
            this.runTaskTimer(plugin, 1L, 1L);
        }

        double getCurrentTPS()
        {
            try {
                final double tps = 1000.0 / this.getDelta();
                return (tps > 20.0) ? 20.0 : tps;
            } catch (Exception e) {
                return 20.0;
            }
        }

        public void run()
        {
            final long curr = System.currentTimeMillis();
            final long delta = curr - this.lastTick;
            this.lastTick = curr;
            this.tickIntervals.removeFirst();
            this.tickIntervals.addLast(delta);
        }

        private double getDelta()
        {
            int base = 0;
            for (final long delta : this.tickIntervals) {
                base += (int) delta;
            }
            return base / this.resolution;
        }
    }
}
