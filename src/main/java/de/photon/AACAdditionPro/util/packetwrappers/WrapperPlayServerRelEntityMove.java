package de.photon.AACAdditionPro.util.packetwrappers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.AACAdditionPro.ServerVersion;

public class WrapperPlayServerRelEntityMove extends WrapperPlayServerEntity implements IWrapperPlayServerEntityOnGround
{
    public static final PacketType TYPE = PacketType.Play.Server.REL_ENTITY_MOVE;

    public WrapperPlayServerRelEntityMove()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayServerRelEntityMove(PacketContainer packet)
    {
        super(packet, TYPE);
    }

    protected WrapperPlayServerRelEntityMove(PacketContainer packet, PacketType packetType)
    {
        super(packet, packetType);
    }

    public double getDx()
    {
        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
                return handle.getBytes().read(0) / 32D;
            case MC111:
            case MC112:
            case MC113:
                // TODO: ACTUALLY VERIFY THAT THESE ARE NOT INTEGERS!!!
                return handle.getShorts().read(1) / 4096D;
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }

    public void setDx(double value)
    {
        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
                handle.getBytes().write(0, (byte) (value * 32));
                break;
            case MC111:
            case MC112:
            case MC113:
                // TODO: ACTUALLY VERIFY THAT THESE ARE NOT INTEGERS!!!
                handle.getShorts().write(1, (short) (value * 4096));
                break;
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }

    public double getDy()
    {
        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
                return handle.getBytes().read(1) / 32D;
            case MC111:
            case MC112:
            case MC113:
                // TODO: ACTUALLY VERIFY THAT THESE ARE NOT INTEGERS!!!
                return handle.getShorts().read(2) / 4096D;
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }

    public void setDy(double value)
    {
        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
                handle.getBytes().write(1, (byte) (value * 32));
                break;
            case MC111:
            case MC112:
            case MC113:
                // TODO: ACTUALLY VERIFY THAT THESE ARE NOT INTEGERS!!!
                handle.getShorts().write(2, (short) (value * 4096));
                break;
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }

    public double getDz()
    {
        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
                return handle.getBytes().read(2) / 32D;
            case MC111:
            case MC112:
            case MC113:
                // TODO: ACTUALLY VERIFY THAT THESE ARE NOT INTEGERS!!!
                return handle.getShorts().read(3) / 4096D;
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }

    public void setDz(double value)
    {
        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
                handle.getBytes().write(2, (byte) (value * 32));
                break;
            case MC111:
            case MC112:
            case MC113:
                // TODO: ACTUALLY VERIFY THAT THESE ARE NOT INTEGERS!!!
                handle.getShorts().write(3, (short) (value * 4096));
                break;
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }

    public void setDiffs(double xDiff, double yDiff, double zDiff)
    {
        setDx(xDiff);
        setDy(yDiff);
        setDz(zDiff);
    }
}
