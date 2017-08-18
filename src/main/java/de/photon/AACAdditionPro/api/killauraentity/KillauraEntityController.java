package de.photon.AACAdditionPro.api.killauraentity;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface KillauraEntityController
{
    /**
     * Controllers get invalidated once another plugin overrides the existing KillauraEntityAddon or the KillauraEntity check gets deactivated
     *
     * @return whether this controller is still valid
     */
    boolean isValid();

    boolean isSpawnAtJoin();

    void setSpawnAtJoin(boolean spawnAtJoin);

    boolean isSpawnedFor(Player player);

    boolean setSpawnedForPlayer(Player player, boolean spawned);

    /**
     * @param spawnLocation used only when the entity is not spawned currently
     *
     * @return whether the operation was successful
     */
    boolean setSpawnedForPlayer(Player player, boolean spawned, Location spawnLocation);
}
