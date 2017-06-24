package de.photon.AACAdditionPro.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class HeuristicsAdditionViolationEvent extends PlayerEvent
{
    private static final HandlerList handlers = new HandlerList();

    private final double confidence;
    private final String pattern;

    public HeuristicsAdditionViolationEvent(final Player p, final double confidence, final String pattern)
    {
        super(p);
        this.confidence = confidence;
        this.pattern = pattern;
    }

    // Needed for 1.8.8
    public static HandlerList getHandlerList()
    {
        return handlers;
    }

    /**
     * Used to get the triggered confidence of a pattern
     *
     * @return the confidence represented by a number in the range of 50 til 100
     */
    public double getConfidence()
    {
        return confidence;
    }

    /**
     * Used to get the triggered pattern
     *
     * @return the confidence represented as a 2 digit-number {@link String}
     */
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