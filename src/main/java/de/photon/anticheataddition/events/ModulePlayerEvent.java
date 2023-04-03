package de.photon.anticheataddition.events;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public abstract class ModulePlayerEvent extends Event implements Cancellable
{
    protected final Player player;
    protected final String moduleId;
    private boolean cancelled = false;
    protected static final HandlerList handlers = new HandlerList();

    // Needed for 1.8.8
    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * Constructor for 1.14 and onwards.
     */
    protected ModulePlayerEvent(final Player p, final String moduleId)
    {
        super(!Bukkit.isPrimaryThread());
        this.player = p;
        this.moduleId = moduleId;
    }

    /**
     * Dummy constructor for legacy minecraft versions before 1.14.
     */
    protected ModulePlayerEvent(final Player player, final String moduleId, boolean legacy)
    {
        super();
        this.player = player;
        this.moduleId = moduleId;
    }

    public ModulePlayerEvent call()
    {
        Bukkit.getPluginManager().callEvent(this);
        return this;
    }

    @Override
    public void setCancelled(final boolean b)
    {
        this.cancelled = b;
    }
}
