package de.photon.aacadditionpro.events;

import de.photon.aacadditionpro.modules.ModuleType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

@SuppressWarnings("unused")
public class PlayerAdditionViolationCommandEvent extends PlayerEvent implements Cancellable
{
    private static final HandlerList handlers = new HandlerList();
    private final ModuleType moduleType;
    private boolean cancelled;
    private String command;

    public PlayerAdditionViolationCommandEvent(final Player player, final String command, final ModuleType moduleType)
    {
        super(player);
        this.command = command;
        this.moduleType = moduleType;
    }

    //Needed for 1.8.8
    public static HandlerList getHandlerList()
    {
        return handlers;
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

    @NotNull
    @Override
    public HandlerList getHandlers()
    {
        return handlers;
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
        return new PlayerAdditionViolationCommandEvent(player, command, moduleType).call();
    }
}
