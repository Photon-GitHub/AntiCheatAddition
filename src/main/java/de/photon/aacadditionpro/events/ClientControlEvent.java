package de.photon.aacadditionpro.events;

import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.util.exceptions.UnknownMinecraftVersion;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

@SuppressWarnings("unused")
public class ClientControlEvent extends Event implements Cancellable
{
    private static final HandlerList handlers = new HandlerList();

    @Getter
    protected final Player player;

    private final ModuleType moduleType;
    private final String message;
    private boolean cancelled = false;

    public static ClientControlEvent build(final Player p, final ModuleType moduleType, final String message)
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
            case MC112:
            case MC113:
                return new ClientControlEvent(p, moduleType, message, true);
            case MC114:
            case MC115:
            case MC116:
                return new ClientControlEvent(p, moduleType, message);
            default:
                throw new UnknownMinecraftVersion();
        }
    }

    public static ClientControlEvent build(final Player p, final ModuleType moduleType)
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
            case MC112:
            case MC113:
                return new ClientControlEvent(p, moduleType, moduleType.getViolationMessage(), true);
            case MC114:
            case MC115:
            case MC116:
                return new ClientControlEvent(p, moduleType, moduleType.getViolationMessage());
            default:
                throw new UnknownMinecraftVersion();
        }
    }

    /**
     * Constructor for 1.14 and onwards.
     */
    protected ClientControlEvent(final Player p, final ModuleType moduleType, final String message)
    {
        super(!Bukkit.isPrimaryThread());
        this.player = p;
        this.moduleType = moduleType;
        this.message = message;
    }

    /**
     * Dummy constructor for legacy minecraft versions before 1.14.
     */
    protected ClientControlEvent(final Player p, final ModuleType moduleType, final String message, boolean legacy)
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

    public ClientControlEvent call()
    {
        Bukkit.getPluginManager().callEvent(this);
        return this;
    }

    public void runIfUncancelled(Consumer<ClientControlEvent> consumer)
    {
        if (!this.isCancelled()) {
            consumer.accept(this);
        }
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
