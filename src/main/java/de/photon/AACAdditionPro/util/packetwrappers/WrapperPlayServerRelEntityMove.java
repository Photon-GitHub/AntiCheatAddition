package de.photon.AACAdditionPro.util.packetwrappers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.AACAdditionPro.AACAdditionPro;

public class WrapperPlayServerRelEntityMove extends WrapperPlayServerEntity implements IWrapperPlayServerEntityOnGround
{
    public static final PacketType TYPE =
            PacketType.Play.Server.REL_ENTITY_MOVE;

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
        switch (AACAdditionPro.getInstance().getServerVersion()) {
            case MC188:
                return handle.getBytes().read(0) / 32D;
            default:
                return handle.getIntegers().read(1) / 4096D;
        }
    }

    public void setDx(double value)
    {
        switch (AACAdditionPro.getInstance().getServerVersion()) {
            case MC188:
                handle.getBytes().write(0, (byte) (value * 32));
                break;
            default:
                handle.getIntegers().write(1, (int) (value * 4096));
                break;
        }
    }

    public double getDy()
    {
        switch (AACAdditionPro.getInstance().getServerVersion()) {
            case MC188:
                return handle.getBytes().read(1) / 32D;
            default:
                return handle.getIntegers().read(2) / 4096D;
        }
    }

    public void setDy(double value)
    {
        switch (AACAdditionPro.getInstance().getServerVersion()) {
            case MC188:
                handle.getBytes().write(1, (byte) (value * 32));
                break;
            default:
                handle.getIntegers().write(2, (int) (value * 4096));
                break;
        }
    }

    public double getDz()
    {
        switch (AACAdditionPro.getInstance().getServerVersion()) {
            case MC188:
                return handle.getBytes().read(2) / 32D;
            default:
                return handle.getIntegers().read(3) / 4096D;
        }
    }

    public void setDz(double value)
    {
        switch (AACAdditionPro.getInstance().getServerVersion()) {
            case MC188:
                handle.getBytes().write(2, (byte) (value * 32));
                break;
            default:
                handle.getIntegers().write(3, (int) (value * 4096));
                break;
        }
    }

    public void setDiffs(double xDiff, double yDiff, double zDiff) {
        setDx(xDiff);
        setDy(yDiff);
        setDz(zDiff);
    }
    /**
     * Retrieve On Ground.
     *
     * @return The current On Ground
     */
    public boolean getOnGround()
    {
        return handle.getBooleans().read(0);
    }

    /**
     * Set On Ground.
     *
     * @param value - new value.
     */
    public void setOnGround(boolean value)
    {
        handle.getBooleans().write(0, value);
    }
}
