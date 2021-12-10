package de.photon.aacadditionpro.protocol.packetwrappers.sentbyserver;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.ServerVersion;

public class WrapperPlayServerRelEntityMove extends WrapperPlayServerEntity implements IWrapperPlayServerRelEntityMove
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

    @Override
    public double getDx()
    {
        return ServerVersion.is18() ?
               handle.getBytes().read(0) / 32D :
               // Integers are ok, even though wiki.vg says short
               handle.getIntegers().read(1) / 4096D;
    }

    @Override
    public void setDx(double value)
    {
        Preconditions.checkArgument(value <= 8, "Tried to move relative " + value + " blocks when teleport is needed.");

        if (ServerVersion.is18()) handle.getBytes().write(0, (byte) (value * 32));
            // Integers are ok, even though wiki.vg says short
        else handle.getIntegers().write(1, (int) (value * 4096));
    }

    @Override
    public double getDy()
    {
        return ServerVersion.is18() ?
               handle.getBytes().read(1) / 32D :
               // Integers are ok, even though wiki.vg says short
               handle.getIntegers().read(2) / 4096D;
    }

    @Override
    public void setDy(double value)
    {
        Preconditions.checkArgument(value <= 8, "Tried to move relative " + value + " blocks when teleport is needed.");

        if (ServerVersion.is18()) handle.getBytes().write(1, (byte) (value * 32));
            // Integers are ok, even though wiki.vg says short
        else handle.getIntegers().write(2, (int) (value * 4096));
    }

    @Override
    public double getDz()
    {
        return ServerVersion.is18() ?
               handle.getBytes().read(2) / 32D :
               // Integers are ok, even though wiki.vg says short
               handle.getIntegers().read(3) / 4096D;
    }

    @Override
    public void setDz(double value)
    {
        Preconditions.checkArgument(value <= 8, "Tried to move relative " + value + " blocks when teleport is needed.");

        if (ServerVersion.is18()) handle.getBytes().write(2, (byte) (value * 32));
            // Integers are ok, even though wiki.vg says short
        else handle.getIntegers().write(3, (int) (value * 4096));
    }
}
