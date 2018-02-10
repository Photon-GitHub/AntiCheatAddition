package de.photon.AACAdditionPro.events;

import de.photon.AACAdditionPro.ModuleType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

@SuppressWarnings("unused")
public class ClientControlEvent extends PlayerEvent implements Cancellable
{
    private static final HandlerList handlers = new HandlerList();
    private final ModuleType moduleType;
    private final String message;
    private boolean cancelled = false;

    public ClientControlEvent(final Player p, final ModuleType moduleType, final String message)
    {
        super(p);
        this.moduleType = moduleType;
        this.message = message;
    }

    public ClientControlEvent(final Player p, final ModuleType moduleType)
    {
        this(p, moduleType, moduleType.getViolationMessage());
    }

    //Needed for 1.8.8
    public static HandlerList getHandlerList()
    {
        return handlers;
    }

    /**
     * This method is used to determine the check which was triggered by the player.
     *
     * @return the {@link ModuleType} of the triggered check
     */
    public ModuleType getModuleType()
    {
        return moduleType;
    }

    /**
     * Used to get the verbose message in order to watch a certain detection
     *
     * @return the verbose message that would be printed without the verbose prefix, player name and the vl.
     */
    public String getMessage()
    {
        return message;
    }

    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }

    @Override
    public boolean isCancelled()
    {
        return cancelled;
    }

    @Override
    public void setCancelled(final boolean b)
    {
        this.cancelled = b;
    }
}
