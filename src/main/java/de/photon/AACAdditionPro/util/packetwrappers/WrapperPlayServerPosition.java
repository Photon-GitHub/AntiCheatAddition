package de.photon.AACAdditionPro.util.packetwrappers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.google.common.collect.ImmutableSet;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Collections;
import java.util.Set;

public class WrapperPlayServerPosition extends AbstractPacket
{
    public static final PacketType TYPE = PacketType.Play.Server.POSITION;

    public WrapperPlayServerPosition()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayServerPosition(final PacketContainer packet)
    {
        super(packet, TYPE);
    }

    /**
     * Retrieve X.
     * <p>
     * Notes: absolute/Relative position
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
     * Retrieve Y.
     * <p>
     * Notes: absolute/Relative position
     *
     * @return The current Y
     */
    public double getY()
    {
        return handle.getDoubles().read(1);
    }

    /**
     * Set Y.
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
     * Notes: absolute/Relative position
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
     * Retrieve Yaw.
     * <p>
     * Notes: absolute/Relative rotation on the X Axis, in degrees
     *
     * @return The current Yaw
     */
    public float getYaw()
    {
        return handle.getFloat().read(0);
    }

    /**
     * Set Yaw.
     *
     * @param value - new value.
     */
    public void setYaw(final float value)
    {
        handle.getFloat().write(0, value);
    }

    /**
     * Retrieve Pitch.
     * <p>
     * Notes: absolute/Relative rotation on the Y Axis, in degrees
     *
     * @return The current Pitch
     */
    public float getPitch()
    {
        return handle.getFloat().read(1);
    }

    /**
     * Set Pitch.
     *
     * @param value - new value.
     */
    public void setPitch(final float value)
    {
        handle.getFloat().write(1, value);
    }

    /**
     * Constructs a new {@link Location} with the information of this packet.
     */
    public Location getLocation(final World world)
    {
        return new Location(world, this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch());
    }

    private static final Class<?> FLAGS_CLASS = MinecraftReflection
            .getMinecraftClass("EnumPlayerTeleportFlags",
                               "PacketPlayOutPosition$EnumPlayerTeleportFlags");

    public enum PlayerTeleportFlag
    {
        X,
        Y,
        Z,
        Y_ROT,
        X_ROT
    }

    private StructureModifier<Set<PlayerTeleportFlag>> getFlagsModifier()
    {
        return handle.getModifier().withType(
                Set.class,
                BukkitConverters.getSetConverter(FLAGS_CLASS, EnumWrappers
                        .getGenericConverter(PlayerTeleportFlag.class)));
    }

    public Set<PlayerTeleportFlag> getFlags()
    {
        return getFlagsModifier().read(0);
    }

    public void setFlags(final Set<PlayerTeleportFlag> value)
    {
        getFlagsModifier().write(0, value);
    }

    /**
     * Sets no relative movement flags.
     */
    public void setNoFlags()
    {
        this.setFlags(Collections.emptySet());
    }

    /**
     * Sets all relative movement flags of this packet, i.e.:
     * {@link PlayerTeleportFlag#X}, <br>
     * {@link PlayerTeleportFlag#Y}, <br>
     * {@link PlayerTeleportFlag#Z}, <br>
     * {@link PlayerTeleportFlag#X_ROT}, <br>
     * {@link PlayerTeleportFlag#Y_ROT}
     */
    public void setAllFlags()
    {
        this.setFlags(ImmutableSet.copyOf(PlayerTeleportFlag.values()));
    }
}