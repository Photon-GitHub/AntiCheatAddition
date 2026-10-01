package de.photon.anticheataddition.user.data.subdata;

/**
 * Tracks the last client-selected slot. A server correction makes the client's next selection ambiguous.
 */
public final class SlotSelectionData
{
    private int lastSlot = -1;

    public synchronized boolean record(int slot)
    {
        final boolean duplicate = slot == lastSlot;
        lastSlot = slot;
        return duplicate;
    }

    public synchronized void reset()
    {
        lastSlot = -1;
    }
}
