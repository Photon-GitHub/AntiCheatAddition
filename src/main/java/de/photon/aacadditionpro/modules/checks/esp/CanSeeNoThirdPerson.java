package de.photon.aacadditionpro.modules.checks.esp;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.function.Function;

class CanSeeNoThirdPerson implements Function<Player, Vector[]>
{
    @Override
    public Vector[] apply(Player player)
    {
        return new Vector[]{player.getEyeLocation().toVector()};
    }
}
