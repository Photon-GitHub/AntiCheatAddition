package de.photon.anticheataddition.util.minecraft.ping;

import org.bukkit.entity.Player;

final class ModernPingProvider implements PingProvider
{
    public int getPing(final Player player)
    {
        return player.getPing();
    }
}
