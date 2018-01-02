package de.photon.AACAdditionPro.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class InventoryHeuristicsEvent extends Event implements Cancellable
{
    private static final HandlerList handlers = new HandlerList();

    //Needed for 1.8.8
    public static HandlerList getHandlerList()
    {
        return handlers;
    }

    private final Player player;
    private final String pattern;
    private final double confidence;

    private boolean cancelled = false;

    public InventoryHeuristicsEvent(Player player, String pattern, double confidence)
    {
        this.player = player;
        this.pattern = pattern;
        this.confidence = confidence;
    }

    public Player getPlayer()
    {
        return player;
    }

    public String getPattern()
    {
        return pattern;
    }

    public double getConfidence()
    {
        return confidence;
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
    public void setCancelled(boolean cancel)
    {
        cancelled = cancel;
    }
}
