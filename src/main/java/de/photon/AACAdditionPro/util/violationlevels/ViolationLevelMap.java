package de.photon.AACAdditionPro.util.violationlevels;

import de.photon.AACAdditionPro.AACAdditionPro;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

class ViolationLevelMap extends ConcurrentHashMap<UUID, Integer> implements Listener
{
    ViolationLevelMap(final long decayTicks)
    {
        super();

        // Might need to have a vl manager without vl decrease
        if (decayTicks > 0)
        {
            //The vl-decrease
            Bukkit.getScheduler().scheduleSyncRepeatingTask(
                    AACAdditionPro.getInstance(),
                    () -> this.forEach((key, value) -> this.put(key, value - 1)),
                    0L, decayTicks);
        }
    }

    @Override
    public Integer put(@NotNull UUID key, @NotNull Integer value)
    {
        return value > 0 ? super.put(key, value) : this.remove(key);
    }

    @EventHandler
    public void on(final PlayerQuitEvent event)
    {
        this.remove(event.getPlayer().getUniqueId());
    }
}
