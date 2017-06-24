package de.photon.AACAdditionPro.events;

import de.photon.AACAdditionPro.AdditionHackType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

@SuppressWarnings("unused")
public class PlayerAdditionViolationCommandEvent extends PlayerEvent implements Cancellable
{
    private static final HandlerList handlers = new HandlerList();
    private final AdditionHackType additionHackType;
    private boolean cancelled;
    private String command;

    public PlayerAdditionViolationCommandEvent(final Player player, final String command, final AdditionHackType additionHackType)
    {
        super(player);
        this.command = command;
        this.additionHackType = additionHackType;
    }

    //Needed for 1.8.8
    public static HandlerList getHandlerList()
    {
        return handlers;
    }

    public AdditionHackType getAdditionHackType()
    {
        return additionHackType;
    }

    public String getCommand()
    {
        return command;
    }

    public void setCommand(final String command)
    {
        this.command = command;
    }

    @Override
    public boolean isCancelled()
    {
        return cancelled;
    }

    @Override
    public void setCancelled(final boolean b)
    {
        cancelled = b;
    }

    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }
}
