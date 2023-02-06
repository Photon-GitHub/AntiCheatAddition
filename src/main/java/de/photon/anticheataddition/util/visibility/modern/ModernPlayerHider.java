package de.photon.anticheataddition.util.visibility.modern;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.util.messaging.Log;
import de.photon.anticheataddition.util.visibility.PlayerInformationHider;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class ModernPlayerHider extends PlayerInformationHider
{
    @Override
    protected void onHide(@NotNull Player observer, @NotNull Set<Player> toHide)
    {
        for (Player player : toHide) {
            observer.hidePlayer(AntiCheatAddition.getInstance(), player);
            Log.finest(() -> "Player " + player.getName() + " has been fully hidden from " + observer.getName());
        }
    }

    @Override
    protected void onReveal(@NotNull Player observer, @NotNull Set<Player> revealed)
    {
        for (Player player : revealed) {
            observer.showPlayer(AntiCheatAddition.getInstance(), player);
            Log.finest(() -> "Player " + player.getName() + " has been revealed to " + observer.getName());
        }
    }

    @Override
    protected Set<ServerVersion> getSupportedVersions()
    {
        return ServerVersion.MC112.getSupVersionsFrom();
    }
}
