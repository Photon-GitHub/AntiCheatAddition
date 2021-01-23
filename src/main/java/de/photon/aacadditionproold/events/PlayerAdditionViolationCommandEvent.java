package de.photon.aacadditionproold.events;

import de.photon.aacadditionproold.ServerVersion;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.util.exceptions.UnknownMinecraftVersion;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

@SuppressWarnings("unused")
public class PlayerAdditionViolationCommandEvent extends Event implements Cancellable
{
    private static final HandlerList handlers = new HandlerList();

    //Needed for 1.8.8
    public static HandlerList getHandlerList()
    {
        return handlers;
    }

    @NotNull
    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }

    private final ModuleType moduleType;
    private boolean cancelled;
    private String command;

    @Getter
    protected final Player player;

    // Legacy constructor
    public PlayerAdditionViolationCommandEvent(final Player player, final String command, final ModuleType moduleType, boolean legacy)
    {
        super();
        this.player = player;
        this.command = command;
        this.moduleType = moduleType;
    }

    // Current constructor
    public PlayerAdditionViolationCommandEvent(final Player player, final String command, final ModuleType moduleType)
    {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.command = command;
        this.moduleType = moduleType;
    }

    public ModuleType getModuleType()
    {
        return moduleType;
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

    /**
     * Handles the spigot api version differences.
     */
    public static PlayerAdditionViolationCommandEvent build(final Player p, final String command, final ModuleType moduleType)
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
            case MC112:
            case MC113:
                return new PlayerAdditionViolationCommandEvent(p, command, moduleType, true);
            case MC114:
            case MC115:
            case MC116:
                return new PlayerAdditionViolationCommandEvent(p, command, moduleType);
            default:
                throw new UnknownMinecraftVersion();
        }
    }

    public PlayerAdditionViolationCommandEvent call()
    {
        Bukkit.getPluginManager().callEvent(this);
        return this;
    }

    public void runIfUncancelled(Consumer<PlayerAdditionViolationCommandEvent> consumer)
    {
        if (!this.isCancelled()) {
            consumer.accept(this);
        }
    }

    /**
     * This creates and immediately calls a {@link PlayerAdditionViolationCommandEvent}.
     *
     * @return true if the command event has been cancelled, else false.
     *
     * @see PlayerAdditionViolationCommandEvent#PlayerAdditionViolationCommandEvent(Player, String, ModuleType)
     */
    public static PlayerAdditionViolationCommandEvent createAndCallCommandEvent(final Player player, final String command, final ModuleType moduleType)
    {
        return PlayerAdditionViolationCommandEvent.build(player, command, moduleType).call();
    }
}
