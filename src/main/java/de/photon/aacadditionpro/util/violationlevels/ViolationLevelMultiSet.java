package de.photon.aacadditionpro.util.violationlevels;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import de.photon.aacadditionproold.AACAdditionPro;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

class ViolationLevelMultiSet implements Listener
{
    @Getter
    private final Multiset<UUID> multiset = ConcurrentHashMultiset.create();

    ViolationLevelMultiSet(final long decayTicks)
    {
        // Might need to have a vl manager without vl decrease
        if (decayTicks > 0) {
            //The vl-decrease
            Bukkit.getScheduler().scheduleSyncRepeatingTask(AACAdditionPro.getInstance(), this::decay, 0L, decayTicks);
        }
    }

    /**
     * Decrements the vl of every player.
     */
    public void decay()
    {
        for (UUID uuid : this.multiset.elementSet()) {
            this.multiset.remove(uuid);
        }
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event)
    {
        this.multiset.setCount(event.getPlayer().getUniqueId(), 0);
    }
}
