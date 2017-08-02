package de.photon.AACAdditionPro.util.entities;

import de.photon.AACAdditionPro.api.KillauraEntityController;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Delegates all calls to another KillauraEntityController like a proxy.
 * Provides the benefit of removing the delegated controller out of the {@link java.lang.reflect.Field} via reflection
 */
public class DelegatingKillauraEntityController implements KillauraEntityController
{
    private KillauraEntityController killauraEntityController;

    public DelegatingKillauraEntityController(KillauraEntityController killauraEntityController)
    {
        this.killauraEntityController = killauraEntityController;
    }

    @Override
    public boolean isValid()
    {
        return killauraEntityController != null && killauraEntityController.isValid();
    }

    @Override
    public boolean isSpawnAtJoin()
    {
        return killauraEntityController.isSpawnAtJoin();
    }

    @Override
    public void setSpawnAtJoin(boolean spawnAtJoin)
    {
        killauraEntityController.setSpawnAtJoin(spawnAtJoin);
    }

    @Override
    public boolean isSpawnedFor(Player player)
    {
        return killauraEntityController.isSpawnedFor(player);
    }

    @Override
    public boolean setSpawnedForPlayer(Player player, boolean spawned)
    {
        return killauraEntityController.setSpawnedForPlayer(player, spawned);
    }

    @Override
    public boolean setSpawnedForPlayer(Player player, boolean spawned, Location spawnLocation)
    {
        return killauraEntityController.setSpawnedForPlayer(player, spawned, spawnLocation);
    }
}
