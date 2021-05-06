package de.photon.aacadditionpro.util.world;

import de.photon.aacadditionproold.util.mathematics.AxisAlignedBB;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

@EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
@ToString
public class Region
{
    /**
     * The {@link World} the region is a part of.
     */
    @Getter private final World world;

    /**
     * The boundaries of the region are stored here.
     */
    private final AxisAlignedBB regionBox;

    public Region(final World world, final double x1, final double z1, final double x2, final double z2)
    {
        this.world = world;
        // From y = MIN_VALUE to y = MAX_VALUE as a region is 2-dimensional and has to cover the entire y-range.
        this.regionBox = new AxisAlignedBB(Double.min(x1, x2),
                                           Double.MIN_VALUE,
                                           Double.min(z1, z2),
                                           //
                                           Double.max(x1, x2),
                                           Double.MAX_VALUE,
                                           Double.max(z1, z2));
    }

    /**
     * Parses a {@link Region} from a {@link String} of the following format:
     * [affected_world] [x1] [z1] [x2] [z2]
     */
    public static Region parseRegion(final String stringToParse)
    {
        // Split the String, the ' ' char is gone after that process.
        final String[] parts = stringToParse.split(" ");

        return new Region(Bukkit.getServer().getWorld(parts[0]),
                          Double.parseDouble(parts[1]),
                          Double.parseDouble(parts[2]),
                          Double.parseDouble(parts[3]),
                          Double.parseDouble(parts[4]));
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
}
