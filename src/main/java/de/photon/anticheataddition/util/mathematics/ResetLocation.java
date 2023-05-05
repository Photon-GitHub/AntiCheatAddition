package de.photon.anticheataddition.util.mathematics;

import org.bukkit.Location;
import org.bukkit.World;

public final class ResetLocation extends Location
{
    private final World resetWorld;
    private final double resetX;
    private final double resetY;
    private final double resetZ;

    public ResetLocation()
    {
        this(null, 0, 0, 0);
    }

    public ResetLocation(Location baseLocation)
    {
        // Deep copy of the base location so that changes to the location passed to this will not cause changes to the base parameter.
        this(baseLocation.getWorld(), baseLocation.getX(), baseLocation.getY(), baseLocation.getZ());
    }

    public ResetLocation(World resetWorld, double resetX, double resetY, double resetZ)
    {
        super(resetWorld, resetX, resetY, resetZ);
        this.resetWorld = resetWorld;
        this.resetX = resetX;
        this.resetY = resetY;
        this.resetZ = resetZ;
    }

    public ResetLocation resetToBase()
    {
        this.setWorld(this.resetWorld);
        this.setX(this.resetX);
        this.setY(this.resetY);
        this.setZ(this.resetZ);
        return this;
    }
}