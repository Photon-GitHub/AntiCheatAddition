package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.user.Data;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.util.visibility.HideMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.WeakHashMap;

public class EspInformationData extends Data implements Listener
{
    public final Map<Player, HideMode> hiddenPlayers = new WeakHashMap<>();

    public EspInformationData(final User user)
    {
        super(user);
        AACAdditionPro.getInstance().registerListener(this);
    }

    @EventHandler
    public void on(final PlayerQuitEvent event)
    {
        hiddenPlayers.remove(event.getPlayer());
    }

    @Override
    public void unregister()
    {
        HandlerList.unregisterAll(this);
        this.hiddenPlayers.clear();
        super.unregister();
    }
}
