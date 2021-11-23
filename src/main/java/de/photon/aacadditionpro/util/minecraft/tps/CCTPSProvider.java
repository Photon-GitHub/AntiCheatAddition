package de.photon.aacadditionpro.util.minecraft.tps;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.util.datastructure.buffer.RingBuffer;
import lombok.val;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * This util provides methods to get information from the server that is usually hidden.
 */
class CCTPSProvider implements TPSProvider
{
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
     * Gets the current TPS of the server.
     */
    public double getTPS()
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
