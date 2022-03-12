package de.photon.anticheataddition.protocol.packetwrappers.sentbyserver;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.google.common.collect.Sets;
import de.photon.anticheataddition.protocol.packetwrappers.AbstractPacket;
import de.photon.anticheataddition.protocol.packetwrappers.IWrapperPlayLook;
import de.photon.anticheataddition.protocol.packetwrappers.IWrapperPlayPosition;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class WrapperPlayServerPosition extends AbstractPacket implements IWrapperPlayPosition, IWrapperPlayLook
{
    public static final PacketType TYPE = PacketType.Play.Server.POSITION;
    private static final Class<?> FLAGS_CLASS = MinecraftReflection.getMinecraftClass(
            "EnumPlayerTeleportFlags",
            "PacketPlayOutPosition$EnumPlayerTeleportFlags");

    public WrapperPlayServerPosition()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayServerPosition(final PacketContainer packet)
    {
        super(packet, TYPE);
    }

    @Override
    public float getYaw()
    {
        return handle.getFloat().read(0);
    }

    @Override
    public void setYaw(final float value)
    {
        handle.getFloat().write(0, value);
    }

    @Override
    public float getPitch()
    {
        return handle.getFloat().read(1);
    }

    @Override
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

    @Override
    public void setWithLocation(final Location location)
    {
        this.setX(location.getX());
        this.setY(location.getY());
        this.setZ(location.getZ());

        this.setYaw(location.getYaw());
        this.setPitch(location.getPitch());
    }

    private StructureModifier<Set<PlayerTeleportFlag>> getFlagsModifier()
    {
        return handle.getSets(EnumWrappers.getGenericConverter(FLAGS_CLASS, PlayerTeleportFlag.class));
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
        this.setFlags(PlayerTeleportFlag.ALL_FLAGS);
    }

    /**
     * Setting a flag means relative teleport and not absolute teleport.
     */
    public enum PlayerTeleportFlag
    {
        X,
        Y,
        Z,
        Y_ROT,
        X_ROT;

        private static final Set<PlayerTeleportFlag> ALL_FLAGS = Sets.immutableEnumSet(EnumSet.allOf(PlayerTeleportFlag.class));
    }
}