package de.photon.AACAdditionPro.events;

import de.photon.AACAdditionPro.modules.ModuleType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class PlayerAdditionViolationEvent extends ClientControlEvent
{
    private static final HandlerList handlers = new HandlerList();

    //Needed for 1.8.8
    public static HandlerList getHandlerList()
    {
        return handlers;
    }

    private final int vl;

    public PlayerAdditionViolationEvent(final Player p, final ModuleType moduleType, final int i, final String message)
    {
        super(p, moduleType, message);
        this.vl = i;
    }

    public PlayerAdditionViolationEvent(final Player p, final ModuleType moduleType, final int i)
    {
        super(p, moduleType);
        this.vl = i;
    }

    /**
     * Used to get the current vl of a player in a certain check. The checks are distinguished by their {@link ModuleType}
     *
     * @return the new vl of the player in the {@link ModuleType}
     */
    public int getVl()
    {
        return vl;
    }

    @NotNull
    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }
}