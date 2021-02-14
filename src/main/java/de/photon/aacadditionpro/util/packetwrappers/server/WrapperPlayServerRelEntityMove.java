package de.photon.aacadditionpro.util.packetwrappers.server;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.google.common.base.Preconditions;
import de.photon.aacadditionproold.ServerVersion;
import de.photon.aacadditionproold.util.exceptions.UnknownMinecraftVersion;

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
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
                return handle.getBytes().read(0) / 32D;
            case MC112:
            case MC113:
            case MC114:
                // Integers are ok, even though wiki.vg says short
                return handle.getIntegers().read(1) / 4096D;
            default:
                throw new UnknownMinecraftVersion();
        }
    }

    @Override
    public void setDx(double value)
    {
        Preconditions.checkArgument(value <= 8, "Tried to move relative " + value + " blocks when teleport is needed.");
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
                handle.getBytes().write(0, (byte) (value * 32));
                break;
            case MC112:
            case MC113:
            case MC114:
            case MC115:
            case MC116:
                // Integers are ok, even though wiki.vg says short
                handle.getIntegers().write(1, (int) (value * 4096));
                break;
            default:
                throw new UnknownMinecraftVersion();
        }
    }

    @Override
    public double getDy()
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
                return handle.getBytes().read(1) / 32D;
            case MC112:
            case MC113:
            case MC114:
            case MC115:
            case MC116:
                // Integers are ok, even though wiki.vg says short
                return handle.getIntegers().read(2) / 4096D;
            default:
                throw new UnknownMinecraftVersion();
        }
    }

    @Override
    public void setDy(double value)
    {
        Preconditions.checkArgument(value <= 8, "Tried to move relative " + value + " blocks when teleport is needed.");
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
                handle.getBytes().write(1, (byte) (value * 32));
                break;
            case MC112:
            case MC113:
            case MC114:
            case MC115:
            case MC116:
                // Integers are ok, even though wiki.vg says short
                handle.getIntegers().write(2, (int) (value * 4096));
                break;
            default:
                throw new UnknownMinecraftVersion();
        }
    }

    @Override
    public double getDz()
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
                return handle.getBytes().read(2) / 32D;
            case MC112:
            case MC113:
            case MC114:
            case MC115:
            case MC116:
                // Integers are ok, even though wiki.vg says short
                return handle.getIntegers().read(3) / 4096D;
            default:
                throw new UnknownMinecraftVersion();
        }
    }

    @Override
    public void setDz(double value)
    {
        Preconditions.checkArgument(value <= 8, "Tried to move relative " + value + " blocks when teleport is needed.");
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
                handle.getBytes().write(2, (byte) (value * 32));
                break;
            case MC112:
            case MC113:
            case MC114:
            case MC115:
            case MC116:
                // Integers are ok, even though wiki.vg says short
                handle.getIntegers().write(3, (int) (value * 4096));
                break;
            default:
                throw new UnknownMinecraftVersion();
        }
    }
}
