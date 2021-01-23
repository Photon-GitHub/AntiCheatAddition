package de.photon.aacadditionproold.util.packetwrappers.client;


import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.aacadditionproold.util.packetwrappers.AbstractPacket;
import de.photon.aacadditionproold.util.packetwrappers.IWrapperPlayPosition;
import org.bukkit.Location;
import org.bukkit.World;

public class WrapperPlayClientPositionLook extends AbstractPacket implements IWrapperPlayClientLook, IWrapperPlayPosition
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
     * Constructs a new {@link Location} with the information of this packet.
     */
    public Location getLocation(final World world)
    {
        return new Location(world, this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch());
    }
}