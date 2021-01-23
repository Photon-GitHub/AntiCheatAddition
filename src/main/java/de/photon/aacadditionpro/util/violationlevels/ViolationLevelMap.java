package de.photon.aacadditionpro.util.violationlevels;

import de.photon.aacadditionpro.AACAdditionPro;
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
        super(1000);

        // Might need to have a vl manager without vl decrease
        if (decayTicks > 0) {
            //The vl-decrease
            Bukkit.getScheduler().scheduleSyncRepeatingTask(AACAdditionPro.getInstance(), this::decay, 0L, decayTicks);
        }
    }

    @Override
    public Integer put(@NotNull UUID key, @NotNull Integer value)
    {
        return value > 0 ? super.put(key, value) : this.remove(key);
    }

    /**
     * Decrements the vl of every player.
     */
    public void decay()
    {
        for (Entry<UUID, Integer> entry : this.entrySet()) {
            this.put(entry.getKey(), entry.getValue() - 1);
        }
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event)
    {
        this.remove(event.getPlayer().getUniqueId());
    }
}
