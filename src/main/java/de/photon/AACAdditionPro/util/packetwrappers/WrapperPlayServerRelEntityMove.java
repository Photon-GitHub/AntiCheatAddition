package de.photon.AACAdditionPro.util.packetwrappers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.AACAdditionPro.AACAdditionPro;
import org.bukkit.World;
import org.bukkit.entity.Entity;

public class WrapperPlayServerRelEntityMove extends AbstractPacket
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

    /**
     * Retrieve Entity ID.
     * <p>
     * Notes: entity's ID
     *
     * @return The current Entity ID
     */
    public int getEntityID()
    {
        return handle.getIntegers().read(0);
    }

    /**
     * Set Entity ID.
     *
     * @param value - new value.
     */
    public void setEntityID(int value)
    {
        handle.getIntegers().write(0, value);
    }

    /**
     * Retrieve the entity of the painting that will be spawned.
     *
     * @param world - the current world of the entity.
     *
     * @return The spawned entity.
     */
    public Entity getEntity(World world)
    {
        return handle.getEntityModifier(world).read(0);
    }

    /**
     * Retrieve the entity of the painting that will be spawned.
     *
     * @param event - the packet event.
     *
     * @return The spawned entity.
     */
    public Entity getEntity(PacketEvent event)
    {
        return getEntity(event.getPlayer().getWorld());
    }

    public int getDx()
    {
        return handle.getIntegers().read(1);
    }

    public void setDx(int value)
    {
        switch (AACAdditionPro.getInstance().getServerVersion()) {
            case MC188:
                //value *= 32;
                handle.getBytes().write(0, (byte) value);
                break;
            case MC110:
            case MC111:
                handle.getIntegers().write(1, value);
                break;
        }
    }

    public int getDy()
    {
        return handle.getIntegers().read(2);
    }

    public void setDy(int value)
    {
        switch (AACAdditionPro.getInstance().getServerVersion()) {
            case MC188:
                //value *= 32;
                handle.getBytes().write(1, (byte) value);
                break;
            case MC110:
            case MC111:
                handle.getIntegers().write(2, value);
                break;
        }
    }

    public int getDz()
    {
        return handle.getIntegers().read(3);
    }

    public void setDz(int value)
    {
        switch (AACAdditionPro.getInstance().getServerVersion()) {
            case MC188:
                //value *= 32;
                handle.getBytes().write(2, (byte) value);
                break;
            case MC110:
            case MC111:
                handle.getIntegers().write(3, value);
                break;
        }
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