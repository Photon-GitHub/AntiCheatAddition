package de.photon.AACAdditionPro.events;

import de.photon.AACAdditionPro.ServerVersion;
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

    public static PlayerAdditionViolationEvent build(final Player p, final ModuleType moduleType, final boolean async, final int i, final String message)
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
            case MC113:
                return new PlayerAdditionViolationEvent(p, moduleType, async, i, message, true);
            case MC114:
            case MC115:
                return new PlayerAdditionViolationEvent(p, moduleType, async, i, message);
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }

    public static PlayerAdditionViolationEvent build(final Player p, final ModuleType moduleType, final boolean async, final int i)
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
            case MC113:
                return new PlayerAdditionViolationEvent(p, moduleType, async, i, moduleType.getViolationMessage(), true);
            case MC114:
            case MC115:
                return new PlayerAdditionViolationEvent(p, moduleType, async, i, moduleType.getViolationMessage());
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }

    /**
     * Constructor for 1.14 and onwards.
     */
    public PlayerAdditionViolationEvent(final Player p, final ModuleType moduleType, final boolean async, final int i, final String message)
    {
        super(p, moduleType, async, message);
        this.vl = i;
    }

    /**
     * Dummy constructor for legacy minecraft versions before 1.14.
     */
    public PlayerAdditionViolationEvent(final Player p, final ModuleType moduleType, final boolean async, final int i, final String message, final boolean legacy)
    {
        super(p, moduleType, async, message, legacy);
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