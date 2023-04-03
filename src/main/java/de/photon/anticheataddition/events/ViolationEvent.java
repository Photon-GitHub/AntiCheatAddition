package de.photon.anticheataddition.events;

import de.photon.anticheataddition.ServerVersion;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ViolationEvent extends ModulePlayerEvent
{

    @Getter private final int vl;

    protected ViolationEvent(Player p, String moduleId, int vl)
    {
        super(p, moduleId);
        this.vl = vl;
    }

    protected ViolationEvent(Player player, String moduleId, int vl, boolean legacy)
    {
        super(player, moduleId, legacy);
        this.vl = vl;
    }

    public static ViolationEvent build(Player player, String moduleId, int vl)
    {
        return ServerVersion.NEW_EVENT_VERSION.activeIsLaterOrEqual() ?
               new ViolationEvent(player, moduleId, vl) :
               new ViolationEvent(player, moduleId, vl, true);
    }

    //Needed for 1.8.8
    public static HandlerList getHandlerList()
    {
        return ViolationEvent.handlers;
    }

        @NotNull
        @Override
        public HandlerList getHandlers() {
            return handlers;
        }
    }


