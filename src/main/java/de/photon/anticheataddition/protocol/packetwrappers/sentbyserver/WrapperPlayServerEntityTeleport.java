package de.photon.anticheataddition.protocol.packetwrappers.sentbyserver;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.anticheataddition.protocol.packetwrappers.AbstractPacket;
import de.photon.anticheataddition.protocol.packetwrappers.IWrapperPlayPosition;
import org.bukkit.Location;

public class WrapperPlayServerEntityTeleport extends AbstractPacket implements IWrapperPlayServerLook, IWrapperPlayPosition
{
    public static final PacketType TYPE = PacketType.Play.Server.ENTITY_TELEPORT;

    public WrapperPlayServerEntityTeleport()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayServerEntityTeleport(PacketContainer packet)
    {
        super(packet, TYPE);
    }

    @Override
    public void setWithLocation(Location location)
    {
        this.setX(location.getX());
        this.setY(location.getY());
        this.setZ(location.getZ());

        this.setYaw(location.getYaw());
        this.setPitch(location.getPitch());
    }
}
