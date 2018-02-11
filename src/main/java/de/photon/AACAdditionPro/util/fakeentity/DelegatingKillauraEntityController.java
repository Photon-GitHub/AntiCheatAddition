package de.photon.AACAdditionPro.util.fakeentity;

import de.photon.AACAdditionPro.api.killauraentity.KillauraEntityController;
import de.photon.AACAdditionPro.api.killauraentity.MovementType;
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
    public MovementType getMovementType()
    {
        return MovementType.STAY;
    }

    @Override
    public boolean setSpawnedForPlayer(Player player, boolean spawned, Location spawnLocation)
    {
        return killauraEntityController.setSpawnedForPlayer(player, spawned, spawnLocation);
    }
}
