package de.photon.AACAdditionPro.util.packetwrappers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.AACAdditionPro.ServerVersion;

public class WrapperPlayServerEntityTeleport extends AbstractPacket implements IWrapperPlayServerEntityLook
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

    public double getX()
    {
        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
                return handle.getIntegers().read(1) / 32.0;
            case MC111:
            case MC112:
            case MC113:
                return handle.getDoubles().read(0);
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }

    public void setX(double value)
    {
        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
                handle.getIntegers().write(1, (int) value * 32);
                break;
            case MC111:
            case MC112:
            case MC113:
                handle.getDoubles().write(0, value);
                break;
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }

    public double getY()
    {
        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
                return handle.getIntegers().read(2) / 32.0;
            case MC111:
            case MC112:
            case MC113:
                return handle.getDoubles().read(1);
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }

    public void setY(double value)
    {
        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
                handle.getIntegers().write(2, (int) value * 32);
                break;
            case MC111:
            case MC112:
            case MC113:
                handle.getDoubles().write(1, value);
                break;
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }

    public double getZ()
    {
        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
                return handle.getIntegers().read(3) / 32.0;
            case MC111:
            case MC112:
            case MC113:
                return handle.getDoubles().read(2);
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }

    public void setZ(double value)
    {
        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
                handle.getIntegers().write(3, (int) value * 32);
                break;
            case MC111:
            case MC112:
            case MC113:
                handle.getDoubles().write(2, value);
                break;
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }
}
