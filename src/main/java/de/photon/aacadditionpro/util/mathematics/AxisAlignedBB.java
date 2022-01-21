package de.photon.aacadditionpro.util.mathematics;

import de.photon.aacadditionpro.util.reflection.ClassReflect;
import de.photon.aacadditionpro.util.reflection.Reflect;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.util.Vector;

import java.util.Objects;

@Getter
@ToString
public class AxisAlignedBB implements Cloneable
{

    private double minX;
    private double minY;
    private double minZ;
    private double maxX;
    private double maxY;
    private double maxZ;

    public static AxisAlignedBB fromNms(Object nmsAABB)
    {
        ClassReflect reflectNmsAABB = Reflect.from(nmsAABB.getClass());

        double minX = reflectNmsAABB.field(0).from(nmsAABB).asDouble();
        double minY = reflectNmsAABB.field(1).from(nmsAABB).asDouble();
        double minZ = reflectNmsAABB.field(2).from(nmsAABB).asDouble();
        double maxX = reflectNmsAABB.field(3).from(nmsAABB).asDouble();
        double maxY = reflectNmsAABB.field(4).from(nmsAABB).asDouble();
        double maxZ = reflectNmsAABB.field(5).from(nmsAABB).asDouble();

        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Construct a new BoundingBox with the min and max coordinates given
     *
     * @param minX Minimum X Coordinate
     * @param minY Minimum Y Coordinate
     * @param minZ Minimum Z Coordinate
     * @param maxX Maximum X Coordinate
     * @param maxY Maximum Y Coordinate
     * @param maxZ Maximum Z Coordinate
     */
    public AxisAlignedBB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ)
    {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    /**
     * Set new bounds
     *
     * @param minX Minimum X Coordinate
     * @param minY Minimum Y Coordinate
     * @param minZ Minimum Z Coordinate
     * @param maxX Maximum X Coordinate
     * @param maxY Maximum Y Coordinate
     * @param maxZ Maximum Z Coordinate
     *
     * @return the Bounding Box with new bounds
     */
    public AxisAlignedBB setBounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ)
    {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        return this;
    }

    /**
     * Set new bounds
     *
     * @param other the other Bounding Box from which we should copy
     *
     * @return the Bounding Box with new bounds
     */
    public AxisAlignedBB setBounds(AxisAlignedBB other)
    {
        this.minX = other.minX;
        this.minY = other.minY;
        this.minZ = other.minZ;
        this.maxX = other.maxX;
        this.maxY = other.maxY;
        this.maxZ = other.maxZ;
        return this;
    }

    /**
     * Add coordinates to the Bounding Box
     *
     * @param x the X coordinate which should be added
     * @param y the Y coordinate which should be added
     * @param z the Z coordinate which should be added
     *
     * @return a new Bounding Box which contains the addition of the coordinates
     */
    public AxisAlignedBB addCoordinatesToNewBox(double x, double y, double z)
    {
        double minX = this.minX;
        double minY = this.minY;
        double minZ = this.minZ;
        double maxX = this.maxX;
        double maxY = this.maxY;
        double maxZ = this.maxZ;

        // Manipulate x axis
        if (x < 0) {
            minX += x;
        } else if (x > 0) {
            maxX += x;
        }

        // Manipulate y axis
        if (y < 0) {
            minY += y;
        } else if (y > 0) {
            maxY += y;
        }

        // Manipulate z axis
        if (z < 0) {
            minZ += z;
        } else if (z > 0) {
            maxZ += z;
        }

        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Grow the Bounding Box and return a new one
     *
     * @param x the X coordinate to grow in both directions
     * @param y the Y coordinate to grow in both directions
     * @param z the Z coordinate to grow in both directions
     *
     * @return a new Bounding Box which has been grown by the amount given
     */
    public AxisAlignedBB grow(double x, double y, double z)
    {
        return new AxisAlignedBB(this.minX - x, this.minY - y, this.minZ - z, this.maxX + x, this.maxY + y, this.maxZ + z);
    }

    /**
     * Expand this Bounding Box by the given coordinates
     *
     * @param x the X coordinate to expand in both directions
     * @param y the Y coordinate to expand in both directions
     * @param z the Z coordinate to expand in both directions
     *
     * @return this modified Bounding Box
     */
    public AxisAlignedBB expand(double x, double y, double z)
    {
        this.minX -= x;
        this.minY -= y;
        this.minZ -= z;
        this.maxX += x;
        this.maxY += y;
        this.maxZ += z;
        return this;
    }

    /**
     * Offset the Bounding Box by the given coordinates
     *
     * @param x the X coordinate for how much we should offset
     * @param y the Y coordinate for how much we should offset
     * @param z the Z coordinate for how much we should offset
     *
     * @return this modified Bounding Box
     */
    public AxisAlignedBB offset(double x, double y, double z)
    {
        this.minX += x;
        this.minY += y;
        this.minZ += z;
        this.maxX += x;
        this.maxY += y;
        this.maxZ += z;
        return this;
    }

    /**
     * Shrink the Bounding Box and return a new one
     *
     * @param x the X coordinate to shrink in both directions
     * @param y the Y coordinate to shrink in both directions
     * @param z the Z coordinate to shrink in both directions
     *
     * @return a new Bounding Box which has been grown by the amount given
     */
    public AxisAlignedBB shrink(double x, double y, double z)
    {
        return new AxisAlignedBB(this.minX + x, this.minY + y, this.minZ + z, this.maxX - x, this.maxY - y, this.maxZ - z);
    }

    /**
     * Contract this Bounding Box by the given coordinates
     *
     * @param x the X coordinate to contract in both directions
     * @param y the Y coordinate to contract in both directions
     * @param z the Z coordinate to contract in both directions
     *
     * @return this modified Bounding Box
     */
    public AxisAlignedBB contract(double x, double y, double z)
    {
        this.minX += x;
        this.minY += y;
        this.minZ += z;
        this.maxX -= x;
        this.maxY -= y;
        this.maxZ -= z;
        return this;
    }

    /**
     * Offset the Bounding Box by the given coordinates and return a new one
     *
     * @param x the X coordinate for how much we should offset
     * @param y the Y coordinate for how much we should offset
     * @param z the Z coordinate for how much we should offset
     *
     * @return a new Bounding Box which has been offset
     */
    public AxisAlignedBB getOffsetBoundingBox(double x, double y, double z)
    {
        return new AxisAlignedBB(this.minX + x, this.minY + y, this.minZ + z, this.maxX + x, this.maxY + y, this.maxZ + z);
    }

    /**
     * Get the offset in x-axis
     *
     * @param bb the bounding box from which we want to know the offset to
     * @param x  default or maximum offset allowed
     *
     * @return offset or capped value
     */
    public double calculateXOffset(AxisAlignedBB bb, double x)
    {
        // Check if we are outside the Y bounds
        if (bb.maxY <= this.minY || bb.minY >= this.maxY) {
            return x;
        }

        // Check if we are outside the Z bounds
        if (bb.maxZ <= this.minZ || bb.minZ >= this.maxZ) {
            return x;
        }

        // Check if we have a positive default offset
        if (x > 0 && bb.maxX <= this.minX) {
            // Get the real offset and cap it at the default offset
            double x1 = this.minX - bb.maxX;
            if (x1 < x) {
                x = x1;
            }
        }

        // Check if we have a negative default offset
        if (x < 0 && bb.minX >= this.maxX) {
            // Get the real offset and cap it at the default offset
            double x2 = this.maxX - bb.minX;
            if (x2 > x) {
                x = x2;
            }
        }

        return x;
    }

    /**
     * Get the offset in y axis
     *
     * @param bb the bounding box from which we want to know the offset to
     * @param y  default or maximum offset allowed
     *
     * @return offset or capped value
     */
    public double calculateYOffset(AxisAlignedBB bb, double y)
    {
        // Check if we are outside the X bounds
        if (bb.maxX <= this.minX || bb.minX >= this.maxX) {
            return y;
        }

        // Check if we are outside the Z bounds
        if (bb.maxZ <= this.minZ || bb.minZ >= this.maxZ) {
            return y;
        }

        // Check if we have a positive default offset
        if (y > 0 && bb.maxY <= this.minY) {
            // Get the real offset and cap it at the default offset
            double y1 = this.minY - bb.maxY;
            if (y1 < y) {
                y = y1;
            }
        }

        // Check if we have a negative default offset
        if (y < 0 && bb.minY >= this.maxY) {
            // Get the real offset and cap it at the default offset
            double y2 = this.maxY - bb.minY;
            if (y2 > y) {
                y = y2;
            }
        }

        return y;
    }

    /**
     * Get the offset in z axis
     *
     * @param bb the bounding box from which we want to know the offset to
     * @param z  default or maximum offset allowed
     *
     * @return offset or capped value
     */
    public double calculateZOffset(AxisAlignedBB bb, double z)
    {
        // Check if we are outside the X bounds
        if (bb.maxX <= this.minX || bb.minX >= this.maxX) {
            return z;
        }

        // Check if we are outside the Y bounds
        if (bb.maxY <= this.minY || bb.minY >= this.maxY) {
            return z;
        }

        // Check if we have a positive default offset
        if (z > 0 && bb.maxZ <= this.minZ) {
            // Get the real offset and cap it at the default offset
            double z1 = this.minZ - bb.maxZ;
            if (z1 < z) {
                z = z1;
            }
        }

        // Check if we have a negative default offset
        if (z < 0 && bb.minZ >= this.maxZ) {
            // Get the real offset and cap it at the default offset
            double z2 = this.maxZ - bb.minZ;
            if (z2 > z) {
                z = z2;
            }
        }

        return z;
    }

    /**
     * Check if we intersect with the given Bounding Box
     *
     * @param bb the other bounding box we want to check for intersection with
     *
     * @return true when the given Bounding Box intersects with this one, false when not
     */
    public boolean intersectsWith(AxisAlignedBB bb)
    {
        if (bb.maxX > this.minX && bb.minX < this.maxX) {
            if (bb.maxY > this.minY && bb.minY < this.maxY) {
                return bb.maxZ > this.minZ && bb.minZ < this.maxZ;
            }
        }

        return false;
    }

    /**
     * Check if the given Vector lies within this Bounding Box
     *
     * @param vector the vector which may or may not be in this Bounding Box
     *
     * @return true when the vector is inside this Bounding Box, false when not
     */
    public boolean isVectorInside(Vector vector)
    {
        return !(vector.getX() <= this.minX || vector.getX() >= this.maxX) &&
               !(vector.getY() <= this.minY || vector.getY() >= this.maxY) &&
               (vector.getZ() > this.minZ || vector.getZ() < this.maxZ);
    }

    /**
     * Get the average edge length of this Bounding Box
     *
     * @return the average edge length
     */
    public double getAverageEdgeLength()
    {
        return (this.maxX - this.minX + this.maxY - this.minY + this.maxZ - this.minZ) / 3;
    }

    @Override
    public AxisAlignedBB clone()
    {
        try {
            AxisAlignedBB clone = (AxisAlignedBB) super.clone();
            return clone.setBounds(this);
        } catch (CloneNotSupportedException e) {
            return new AxisAlignedBB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AxisAlignedBB that = (AxisAlignedBB) o;
        return Double.compare(that.minX, minX) == 0 &&
               Double.compare(that.minY, minY) == 0 &&
               Double.compare(that.minZ, minZ) == 0 &&
               Double.compare(that.maxX, maxX) == 0 &&
               Double.compare(that.maxY, maxY) == 0 &&
               Double.compare(that.maxZ, maxZ) == 0;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
