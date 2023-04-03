package de.photon.anticheataddition.events;

import de.photon.anticheataddition.ServerVersion;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class SentinelEvent extends ModulePlayerEvent
{

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
        return ServerVersion.NEW_EVENT_VERSION.activeIsLaterOrEqual() ?
               new SentinelEvent(player, moduleId) :
               new SentinelEvent(player, moduleId, true);
    }

    //Needed for 1.8.8
    public static HandlerList getHandlerList()
    {
        return SentinelEvent.handlers;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}

