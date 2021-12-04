package de.photon.aacadditionpro.protocol.packetwrappers.sentbyserver;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.protocol.packetwrappers.AbstractPacket;
import de.photon.aacadditionpro.protocol.packetwrappers.IWrapperPlayPosition;
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

    @Override
    public double getX()
    {
        return ServerVersion.getActiveServerVersion() == ServerVersion.MC18 ?
               handle.getIntegers().read(1) / 32.0 :
               handle.getDoubles().read(0);
    }

    @Override
    public void setX(double value)
    {
        if (ServerVersion.getActiveServerVersion() == ServerVersion.MC18) handle.getIntegers().write(1, (int) value * 32);
        else handle.getDoubles().write(0, value);
    }

    @Override
    public double getY()
    {
        return ServerVersion.getActiveServerVersion() == ServerVersion.MC18 ?
               handle.getIntegers().read(2) / 32.0 :
               handle.getDoubles().read(1);
    }

    @Override
    public void setY(double value)
    {
        if (ServerVersion.getActiveServerVersion() == ServerVersion.MC18) handle.getIntegers().write(2, (int) value * 32);
        else handle.getDoubles().write(1, value);
    }

    @Override
    public double getZ()
    {
        return ServerVersion.getActiveServerVersion() == ServerVersion.MC18 ?
               handle.getIntegers().read(3) / 32.0 :
               handle.getDoubles().read(2);
    }

    @Override
    public void setZ(double value)
    {
        if (ServerVersion.getActiveServerVersion() == ServerVersion.MC18) handle.getIntegers().write(3, (int) value * 32);
        else handle.getDoubles().write(2, value);
    }
}
