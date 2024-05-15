package de.photon.anticheataddition.util.log;

import de.photon.anticheataddition.events.SentinelEvent;
import de.photon.anticheataddition.events.ViolationEvent;
import de.photon.anticheataddition.util.execute.Placeholders;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

final class ViolationLogger implements Listener
{
    private static final String SENTINEL_PRE_STRING = (ChatColor.WHITE + "{player} " + ChatColor.GRAY) + "Sentinel detection: ";
    private static final String VIOLATION_PRE_STRING = (ChatColor.WHITE + "{player} " + ChatColor.GRAY) + "module detection: ";

    @EventHandler
    public void onAdditionViolation(final ViolationEvent event)
    {
        Log.fine(() -> Placeholders.replacePlaceholders(VIOLATION_PRE_STRING + event.getModuleId() + " | added vl: " + event.getVl() + " | TPS: {tps} | Ping: {ping}", event.getPlayer()));
    }

    @EventHandler
    public void onClientControl(final SentinelEvent event)
    {
        Log.fine(() -> Placeholders.replacePlaceholders(SENTINEL_PRE_STRING + event.getModuleId(), event.getPlayer()));
    }
}
