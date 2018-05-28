package de.photon.AACAdditionPro.util.packetwrappers;


import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Location;
import org.bukkit.World;

public class WrapperPlayClientPositionLook extends AbstractPacket implements IWrapperPlayClientLook
{
    public static final PacketType TYPE = PacketType.Play.Client.POSITION_LOOK;

    public WrapperPlayClientPositionLook()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayClientPositionLook(final PacketContainer packet)
    {
        super(packet, TYPE);
    }

    /**
     * Retrieve X.
     * <p>
     * Notes: absolute position
     *
     * @return The current X
     */
    public double getX()
    {
        return handle.getDoubles().read(0);
    }

    /**
     * Set X.
     *
     * @param value - new value.
     */
    public void setX(final double value)
    {
        handle.getDoubles().write(0, value);
    }

    /**
     * Retrieve Feet Y.
     * <p>
     * Notes: absolute feet position. Is normally HeadY - 1.62. Used to modify
     * the players bounding box when going up stairs, crouching, etc…
     *
     * @return The current FeetY
     */
    public double getY()
    {
        return handle.getDoubles().read(1);
    }

    /**
     * Set Feet Y.
     *
     * @param value - new value.
     */
    public void setY(final double value)
    {
        handle.getDoubles().write(1, value);
    }

    /**
     * Retrieve Z.
     * <p>
     * Notes: absolute position
     *
     * @return The current Z
     */
    public double getZ()
    {
        return handle.getDoubles().read(2);
    }

    /**
     * Set Z.
     *
     * @param value - new value.
     */
    public void setZ(final double value)
    {
        handle.getDoubles().write(2, value);
    }

    /**
     * Constructs a new {@link Location} with the information of this packet.
     */
    public Location getLocation(final World world)
    {
        return new Location(world, this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch());
    }
}