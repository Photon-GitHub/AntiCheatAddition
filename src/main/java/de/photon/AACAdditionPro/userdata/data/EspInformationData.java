package de.photon.AACAdditionPro.userdata.data;

import de.photon.AACAdditionPro.userdata.Data;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.util.visibility.HideMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.WeakHashMap;

public class EspInformationData extends Data
{
    public final Map<Player, HideMode> hiddenPlayers = new WeakHashMap<>();

    public EspInformationData(final User theUser)
    {
        super(true, theUser);
    }

    @EventHandler
    @Override
    public void on(final PlayerQuitEvent event)
    {
        hiddenPlayers.remove(event.getPlayer());
        // Listener cleanup
        super.on(event);
    }
}
