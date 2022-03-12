package de.photon.anticheataddition.util.datastructure.broadcast;

import com.google.common.collect.Sets;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * This class broadcasts a message of a certain type to receivers.
 */
@EqualsAndHashCode(doNotUseGetters = true)
public class Broadcaster<T>
{
    // Receivers should not change often -> CopyOnWriteArraySet.
    // Thread safety is guaranteed by the CopyOnWriteArraySet.
    @NotNull private final Set<BroadcastReceiver<T>> receivers = Sets.newCopyOnWriteArraySet();

    /**
     * Sends a value of a previously defined type to all subscribed {@link BroadcastReceiver}s.
     */
    public void broadcast(T value)
    {
        for (BroadcastReceiver<T> receiver : this.receivers) receiver.receive(value);
    }

    /**
     * Add a {@link BroadcastReceiver} to the recipients of a broadcast.
     */
    public void subscribe(BroadcastReceiver<T> receiver)
    {
        this.receivers.add(receiver);
    }

    /**
     * Remove a {@link BroadcastReceiver} from the recipients of a broadcast.
     */
    public void unsubscribe(BroadcastReceiver<T> receiver)
    {
        this.receivers.remove(receiver);
    }
}