package de.photon.anticheataddition.events;

import de.photon.anticheataddition.ServerVersion;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ViolationEvent extends ModulePlayerEvent
{
    private static final HandlerList handlers = new HandlerList();

    @Getter private final int vl;

    protected ViolationEvent(Player p, String moduleId, int vl)
    {
        super(p, moduleId);
        this.vl = vl;
    }

    protected ViolationEvent(Player player, String moduleId, int vl, boolean legacy)
    {
        super(player, moduleId, legacy);
        this.vl = vl;
    }

    public static ViolationEvent build(Player player, String moduleId, int vl)
    {
        return ServerVersion.containsActiveServerVersion(ServerVersion.LEGACY_EVENT_VERSIONS) ?
               new ViolationEvent(player, moduleId, vl, true) :
               new ViolationEvent(player, moduleId, vl);
    }

    //Needed for 1.8.8
    public static HandlerList getHandlerList()
    {
        return ViolationEvent.handlers;
    }

    @NotNull
    @Override
    public HandlerList getHandlers()
    {
        return ViolationEvent.handlers;
    }
}
