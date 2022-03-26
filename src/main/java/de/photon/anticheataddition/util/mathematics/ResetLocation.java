package de.photon.anticheataddition.util.mathematics;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;

public class ResetLocation extends Location
{
    @Getter
    private final Location baseLocation;

    public ResetLocation()
    {
        this(null, 0, 0, 0);
    }

    public ResetLocation(Location baseLocation)
    {
        super(baseLocation.getWorld(), baseLocation.getX(), baseLocation.getY(), baseLocation.getZ());
        this.baseLocation = baseLocation;
    }

    public ResetLocation(World world, double resetX, double resetY, double resetZ)
    {
        super(world, resetX, resetY, resetZ);
        this.baseLocation = new Location(world, resetX, resetY, resetZ);
    }

    public ResetLocation resetToBase()
    {
        this.setWorld(this.baseLocation.getWorld());
        this.setX(this.baseLocation.getX());
        this.setY(this.baseLocation.getY());
        this.setZ(this.baseLocation.getZ());
        return this;
    }

    @Override
    public boolean equals(Object o)
    {
        return super.equals(o);
    }

    @Override
    public int hashCode()
    {
        return super.hashCode();
    }
}