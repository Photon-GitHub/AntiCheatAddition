package de.photon.aacadditionpro.util.server.ping;

import org.bukkit.entity.Player;

public class ModernPingProvider implements PingProvider
{
    public int getPing(final Player player)
    {
        return player.getPing();
    }
}
