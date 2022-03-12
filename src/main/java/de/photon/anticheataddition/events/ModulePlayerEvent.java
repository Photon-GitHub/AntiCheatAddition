package de.photon.anticheataddition.events;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

import java.util.function.Consumer;

@Getter
public abstract class ModulePlayerEvent extends Event implements Cancellable
{
    protected final Player player;
    protected final String moduleId;
    private boolean cancelled = false;

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

    public void runIfUncancelled(Consumer<ModulePlayerEvent> consumer)
    {
        if (!this.isCancelled()) consumer.accept(this);
    }


    @Override
    public void setCancelled(final boolean b)
    {
        this.cancelled = b;
    }
}
