package de.photon.aacadditionpro.events;

import de.photon.aacadditionpro.ServerVersion;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class SentinelEvent extends ModulePlayerEvent
{
    private static final HandlerList handlers = new HandlerList();

    protected SentinelEvent(Player p, String moduleId)
    {
        super(p, moduleId);
    }

    protected SentinelEvent(Player player, String moduleId, boolean legacy)
    {
        super(player, moduleId, legacy);
    }

    public static SentinelEvent build(Player player, String moduleId)
    {
        return ServerVersion.containsActiveServerVersion(ServerVersion.LEGACY_EVENT_VERSIONS) ?
               new SentinelEvent(player, moduleId, true) :
               new SentinelEvent(player, moduleId);
    }

    //Needed for 1.8.8
    public static HandlerList getHandlerList()
    {
        return SentinelEvent.handlers;
    }

    @NotNull
    @Override
    public HandlerList getHandlers()
    {
        return SentinelEvent.handlers;
    }
}
