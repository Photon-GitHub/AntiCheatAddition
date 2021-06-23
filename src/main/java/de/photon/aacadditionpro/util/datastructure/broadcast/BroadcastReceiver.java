package de.photon.aacadditionpro.util.datastructure.broadcast;

/**
 * Defines a receiver for messages of a {@link Broadcaster}.
 */
public interface BroadcastReceiver<T>
{
    /**
     * Receive a value.
     * This method may be called asynchronously.
     */
    void receive(T value);
}
