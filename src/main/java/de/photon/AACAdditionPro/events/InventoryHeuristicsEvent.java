package de.photon.AACAdditionPro.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class InventoryHeuristicsEvent extends Event
{
    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final boolean training;
    private final String pattern;

    public InventoryHeuristicsEvent(final Player player, final boolean training, final String pattern)
    {
        this.player = player;
        this.training = training;
        this.pattern = pattern;
    }

    //Needed for 1.8.8
    public static HandlerList getHandlerList()
    {
        return handlers;
    }

    public Player getPlayer()
    {
        return player;
    }

    public boolean isTraining()
    {
        return training;
    }

    public String getPattern()
    {
        return pattern;
    }

    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }
}
