package de.photon.anticheataddition.protocol.packetwrappers.sentbyclient;


import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerAction;
import de.photon.anticheataddition.protocol.packetwrappers.AbstractPacket;
import de.photon.anticheataddition.protocol.packetwrappers.IWrapperPlayEntity;

public class WrapperPlayClientEntityAction extends AbstractPacket implements IWrapperPlayEntity
{
    public static final PacketType TYPE = PacketType.Play.Client.ENTITY_ACTION;

    public WrapperPlayClientEntityAction()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayClientEntityAction(PacketContainer packet)
    {
        super(packet, TYPE);
    }

    /**
     * Retrieve Action ID.
     * <p>
     * Notes: the ID of the action, see below.
     *
     * @return The current Action ID
     */
    public PlayerAction getAction()
    {
        return handle.getPlayerActions().read(0);
    }

    /**
     * Set Action ID.
     *
     * @param value - new value.
     */
    public void setAction(PlayerAction value)
    {
        handle.getPlayerActions().write(0, value);
    }

    /**
     * Retrieve Jump Boost.
     * <p>
     * Notes: horse jump boost. Ranged from 0 -> 100.
     *
     * @return The current Jump Boost
     */
    public int getJumpBoost()
    {
        return handle.getIntegers().read(1);
    }

    /**
     * Set Jump Boost.
     *
     * @param value - new value.
     */
    public void setJumpBoost(int value)
    {
        handle.getIntegers().write(1, value);
    }

}