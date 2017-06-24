package de.photon.AACAdditionPro.util.packetwrappers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.AACAdditionPro.AACAdditionPro;
import org.bukkit.World;
import org.bukkit.entity.Entity;

public class WrapperPlayServerRelEntityMoveLook extends AbstractPacket
{
    public static final PacketType TYPE =
            PacketType.Play.Server.REL_ENTITY_MOVE_LOOK;

    public WrapperPlayServerRelEntityMoveLook()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayServerRelEntityMoveLook(PacketContainer packet)
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

    /**
     * Retrieve DX.
     *
     * @return The current DX
     */
    public double getDx()
    {
        return handle.getIntegers().read(1) / 4096D;
    }

    /**
     * Set DX.
     *
     * @param value - new value.
     */
    public void setDx(double value)
    {
        switch (AACAdditionPro.getInstance().getServerVersion()) {
            case MC188:
                //value *= 32;
            case MC110:
            case MC111:
                handle.getIntegers().write(1, (int) (value * 4096));
                break;
        }
    }

    /**
     * Retrieve DY.
     *
     * @return The current DY
     */
    public double getDy()
    {
        return handle.getIntegers().read(2) / 4096D;
    }

    /**
     * Set DY.
     *
     * @param value - new value.
     */
    public void setDy(double value)
    {
        switch (AACAdditionPro.getInstance().getServerVersion()) {
            case MC188:
                //value *= 32;
            case MC110:
            case MC111:
                handle.getIntegers().write(1, (int) (value * 4096));
                break;
        }
    }

    /**
     * Retrieve DZ.
     *
     * @return The current DZ
     */
    public double getDz()
    {
        return handle.getIntegers().read(3) / 4096D;
    }

    /**
     * Set DZ.
     *
     * @param value - new value.
     */
    public void setDz(double value)
    {
        switch (AACAdditionPro.getInstance().getServerVersion()) {
            case MC188:
                //value *= 32;
            case MC110:
            case MC111:
                handle.getIntegers().write(1, (int) (value * 4096));
                break;
        }
    }

    /**
     * Retrieve the yaw of the current entity.
     *
     * @return The current Yaw
     */
    public float getYaw()
    {
        return (handle.getBytes().read(0) * 360.F) / 256.0F;
    }

    /**
     * Set the yaw of the current entity.
     *
     * @param value - new yaw.
     */
    public void setYaw(float value)
    {
        handle.getBytes().write(0, (byte) (value * 256.0F / 360.0F));
    }

    /**
     * Retrieve the pitch of the current entity.
     *
     * @return The current pitch
     */
    public float getPitch()
    {
        return (handle.getBytes().read(1) * 360.F) / 256.0F;
    }

    /**
     * Set the pitch of the current entity.
     *
     * @param value - new pitch.
     */
    public void setPitch(float value)
    {
        handle.getBytes().write(1, (byte) (value * 256.0F / 360.0F));
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