package de.photon.aacadditionpro.util.minecraft.ping;

import org.bukkit.entity.Player;

class ModernPingProvider implements PingProvider
{
    public int getPing(final Player player)
    {
        return player.getPing();
    }
}
