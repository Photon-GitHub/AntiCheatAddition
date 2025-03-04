package de.photon.anticheataddition.util.minecraft.tps;

import com.tcoded.folialib.FoliaLib;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.util.datastructure.statistics.MovingLongStatistics;
import org.bukkit.Bukkit;

/**
 * This util provides methods to get information from the server that is usually hidden.
 */
final class ModernTPSProvider implements TPSProvider
{
    private static final int RESOLUTION = 40;

    // Init the RingBuffer with 50 millis for 20 TPS.
    private final MovingLongStatistics tickIntervals = new MovingLongStatistics(RESOLUTION, 50L);
    private long lastTick = System.currentTimeMillis();

    public ModernTPSProvider()
    {
        FoliaLib foliaLib = AntiCheatAddition.getInstance().getFoliaLib();
        if (foliaLib.isFolia())
        {
            foliaLib.getScheduler().runTimer(() -> {
                final long curr = System.currentTimeMillis();
                tickIntervals.add(curr - this.lastTick);
                this.lastTick = curr;
            }, 1, 1);
        }else {
            Bukkit.getScheduler().runTaskTimer(AntiCheatAddition.getInstance(), () -> {
            final long curr = System.currentTimeMillis();
            // Add the tick time difference as a data point.
            tickIntervals.add(curr - this.lastTick);
            this.lastTick = curr;
            }, 1L, 1L);
        }

    }

    @Override
    public double getTPS()
    {
        final double average = this.tickIntervals.getAverage();
        // 1000 milliseconds per second, average is also milliseconds -> ticks. As the maximum of ticks is 20, allow no value above 20.
        return average <= 0 ? 20.0 : Math.min(1000.0 / average, 20.0);
    }
}
