package de.photon.aacadditionpro.modules.additions.esp;

import org.bukkit.Location;
import org.bukkit.entity.Player;

class SingleCameraSupplier implements CameraVectorSupplier
{
    @Override
    public Location[] getCameraLocations(Player player)
    {
        return new Location[]{player.getEyeLocation()};
    }
}
