package de.photon.AACAdditionPro.events;

import de.photon.AACAdditionPro.AdditionHackType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

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

    public PlayerAdditionViolationEvent(final Player p, final AdditionHackType additionHackType, final int i, final String message)
    {
        super(p, additionHackType, message);
        this.vl = i;
    }

    public PlayerAdditionViolationEvent(final Player p, final AdditionHackType additionHackType, final int i)
    {
        super(p, additionHackType);
        this.vl = i;
    }

    /**
     * Used to get the current vl of a player in a certain check. The checks are distinguished by their {@link AdditionHackType}
     *
     * @return the new vl of the player in the {@link AdditionHackType}
     */
    public int getVl()
    {
        return vl;
    }
    
    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }
}