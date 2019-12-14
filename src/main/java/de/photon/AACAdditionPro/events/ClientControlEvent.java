package de.photon.AACAdditionPro.events;

import de.photon.AACAdditionPro.ServerVersion;
import de.photon.AACAdditionPro.modules.ModuleType;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class ClientControlEvent extends Event implements Cancellable
{
    private static final HandlerList handlers = new HandlerList();

    @Getter
    protected final Player player;

    private final ModuleType moduleType;
    private final String message;
    private boolean cancelled = false;

    public static ClientControlEvent build(final Player p, final ModuleType moduleType, boolean async, final String message)
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
            case MC113:
                return new ClientControlEvent(p, moduleType, async, message, true);
            case MC114:
                return new ClientControlEvent(p, moduleType, async, message);
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }

    public static ClientControlEvent build(final Player p, final ModuleType moduleType, boolean async)
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
            case MC113:
                return new ClientControlEvent(p, moduleType, async, moduleType.getViolationMessage(), true);
            case MC114:
                return new ClientControlEvent(p, moduleType, async, moduleType.getViolationMessage());
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }

    /**
     * Constructor for 1.14 and onwards.
     */
    protected ClientControlEvent(final Player p, final ModuleType moduleType, boolean async, final String message)
    {
        super(async);
        this.player = p;
        this.moduleType = moduleType;
        this.message = message;
    }

    /**
     * Dummy constructor for legacy minecraft versions before 1.14.
     */
    protected ClientControlEvent(final Player p, final ModuleType moduleType, boolean async, final String message, boolean legacy)
    {
        super();
        this.player = p;
        this.moduleType = moduleType;
        this.message = message;
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

    @NotNull
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
