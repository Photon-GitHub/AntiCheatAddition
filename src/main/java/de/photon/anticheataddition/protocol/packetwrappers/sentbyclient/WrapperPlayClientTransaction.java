package de.photon.anticheataddition.protocol.packetwrappers.sentbyclient;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.anticheataddition.protocol.packetwrappers.AbstractPacket;

public class WrapperPlayClientTransaction extends AbstractPacket
{
    public static final PacketType TYPE = PacketType.Play.Client.TRANSACTION;

    public WrapperPlayClientTransaction() {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayClientTransaction(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieve Window ID.
     * <p>
     * Notes: the id of the window that the action occurred in.
     *
     * @return The current Window ID
     */
    public int getWindowId() {
        return handle.getIntegers().read(0);
    }

    /**
     * Set Window ID.
     *
     * @param value - new value.
     */
    public void setWindowId(byte value) {
        handle.getIntegers().write(0, (int) value);
    }

    /**
     * Retrieve Action number.
     * <p>
     * Notes: every action that is to be accepted has a unique number. This
     * field corresponds to that number.
     *
     * @return The current Action number
     */
    public short getActionNumber() {
        return handle.getShorts().read(0);
    }

    /**
     * Set Action number.
     *
     * @param value - new value.
     */
    public void setActionNumber(short value) {
        handle.getShorts().write(0, value);
    }

    /**
     * Retrieve Accepted.
     * <p>
     * Notes: whether the action was accepted.
     *
     * @return The current Accepted
     */
    public boolean getAccepted() {
        return handle.getBooleans().read(0);
    }

    /**
     * Set Accepted.
     *
     * @param value - new value.
     */
    public void setAccepted(boolean value) {
        handle.getBooleans().write(0, value);
    }

}