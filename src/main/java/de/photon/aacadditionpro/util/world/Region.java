package de.photon.aacadditionpro.util.world;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.util.mathematics.AxisAlignedBB;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;

public class Region
{
    /**
     * The {@link World} the region is a part of.
     */
    @Getter
    private final World world;

    /**
     * The boundaries of the world are stored here.
     */
    private AxisAlignedBB regionBox;

    public Region(final World world, final double x1, final double z1, final double x2, final double z2)
    {
        this.world = world;
        this.constructRegionBox(x1, z1, x2, z2);
    }

    /**
     * Constructs the {@link AxisAlignedBB} of this region.
     */
    private void constructRegionBox(final double x1, final double z1, final double x2, final double z2)
    {
        // Make sure the coords are sorted the right way.
        double[] minCoords = new double[2];
        double[] maxCoords = new double[2];

        if (x1 < x2)
        {
            minCoords[0] = x1;
            maxCoords[0] = x2;
        }
        else
        {
            minCoords[0] = x2;
            maxCoords[0] = x1;
        }

        if (z1 < z2)
        {
            minCoords[1] = z1;
            maxCoords[1] = z2;
        }
        else
        {
            minCoords[1] = z2;
            maxCoords[1] = z1;
        }

        this.regionBox = new AxisAlignedBB(minCoords[0], Double.MIN_VALUE, minCoords[1], minCoords[1], Double.MAX_VALUE, maxCoords[1]);
    }

    /**
     * Determines whether a {@link Location} is inside this {@link Region}
     *
     * @param location the {@link Location} that should be checked if it lies inside this {@link Region}
     *
     * @return if the given {@link Location} is inside this {@link Region}.
     */
    public boolean isInsideRegion(final Location location)
    {
        return this.world.equals(location.getWorld()) && regionBox.isVectorInside(location.toVector());
    }

    /**
     * Parses a {@link Region} from a {@link String} of the following format: <br></>
     * <affected_world> <x1> <z1> <x2> <z2>
     */
    public static Region parseRegion(final String stringToParse)
    {
        // Split the String, the ' ' char is gone after that process.
        final String[] parts = stringToParse.split(" ");

        return new Region(AACAdditionPro.getInstance().getServer().getWorld(parts[0]),
                          Double.parseDouble(parts[1]),
                          Double.parseDouble(parts[2]),
                          Double.parseDouble(parts[3]),
                          Double.parseDouble(parts[4]));
    }
}
