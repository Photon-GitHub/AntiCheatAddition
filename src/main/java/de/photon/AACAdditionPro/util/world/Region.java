package de.photon.AACAdditionPro.util.world;

import de.photon.AACAdditionPro.AACAdditionPro;
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
     * The corners of the world are stored here.
     * [0] == x1
     * [1] == z1
     * [2] == x2
     * [3] == z2
     */
    @Getter
    private final double[] corners;

    public Region(final World world, final double x1, final double z1, final double x2, final double z2)
    {
        this.world = world;
        this.corners = new double[]{
                x1,
                z1,
                x2,
                z2
        };
    }

    /* Needs a String in the following format:
    - <affected_world> <x1> <z1> <x2> <z2> */
    public Region(final String toParseString)
    {
        // Split the String, the ' ' char is gone after that process.
        final String[] parts = toParseString.split(" ");
        this.world = AACAdditionPro.getInstance().getServer().getWorld(parts[0]);

        // Init the corners
        this.corners = new double[4];
        for (byte b = 0; b < 4; b++) {
            corners[b] = Double.parseDouble(parts[b + 1]);
        }
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
        if (this.corners[0] > this.corners[2]) {
            if (location.getX() < corners[2] || location.getX() > corners[0]) {
                return false;
            }
        } else {
            if (location.getX() < corners[0] || location.getX() > corners[2]) {
                return false;
            }
        }

        if (this.corners[1] > this.corners[3]) {
            if (location.getZ() < corners[3] || location.getZ() > corners[1]) {
                return false;
            }
        } else {
            if (location.getZ() < corners[1] || location.getZ() > corners[3]) {
                return false;
            }
        }

        return true;
    }
}
