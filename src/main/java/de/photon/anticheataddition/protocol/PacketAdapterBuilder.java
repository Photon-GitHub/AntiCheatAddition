package de.photon.anticheataddition.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.base.Preconditions;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.messaging.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class PacketAdapterBuilder
{
    @NotNull private final Module module;
    @NotNull private final Set<PacketType> types;

    private ListenerPriority priority = ListenerPriority.NORMAL;
    private BiConsumer<PacketEvent, User> onReceiving = null;
    private BiConsumer<PacketEvent, User> onSending = null;

    public static boolean checkSync(@NotNull Callable<Boolean> task)
    {
        // Dummy timeout that will be caught in the method.
        return checkSync(-1, TimeUnit.NANOSECONDS, task);
    }

    /**
     * This method uses the {@link org.bukkit.scheduler.BukkitScheduler#callSyncMethod(Plugin, Callable)} method to calculate a certain boolean expression on the main server thread.
     * This is necessary for all potentially chunk-loading operations.
     *
     * @param task    the boolean expression to evaluate
     * @param timeout the timeout after which the calculation shall be stopped. Negative timeout will wait indefinitely.
     * @param unit    the {@link TimeUnit} for timeout.
     */
    public static boolean checkSync(long timeout, TimeUnit unit, @NotNull Callable<Boolean> task)
    {
        try {
            // If the timeout is smaller than or equal to 0, wait indefinitely.
            return timeout <= 0 ?
                   Boolean.TRUE.equals(Bukkit.getScheduler().callSyncMethod(AntiCheatAddition.getInstance(), task).get()) :
                   Boolean.TRUE.equals(Bukkit.getScheduler().callSyncMethod(AntiCheatAddition.getInstance(), task).get(timeout, unit));

        } catch (InterruptedException | ExecutionException e) {
            Log.error("Unable to complete the synchronous calculations.", e);
            Thread.currentThread().interrupt();
        } catch (TimeoutException e) {
            Log.severe(() -> "Unable to finish synchronous calculations. If this message appears frequently please consider upgrading your server.");
        }
        return false;
    }

    public static PacketAdapterBuilder of(@NotNull Module module, @NotNull PacketType... types)
    {
        return of(module, Set.of(types));
    }

    public static PacketAdapterBuilder of(@NotNull Module module, @NotNull Set<PacketType> types)
    {
        Preconditions.checkNotNull(types, "Tried to create PacketAdapterBuilder with null types.");
        Preconditions.checkArgument(!types.isEmpty(), "Tried to create PacketAdapterBuilder without types.");
        return new PacketAdapterBuilder(module, types);
    }

    public PacketAdapterBuilder priority(ListenerPriority priority)
    {
        Preconditions.checkNotNull(priority, "Tried to set PacketAdapterBuilder ListenerPriority to null.");
        this.priority = priority;
        return this;
    }

    public PacketAdapterBuilder onReceiving(BiConsumer<PacketEvent, User> onReceiving)
    {
        this.onReceiving = onReceiving;
        return this;
    }

    public PacketAdapterBuilder onSending(BiConsumer<PacketEvent, User> onSending)
    {
        this.onSending = onSending;
        return this;
    }

    private void runPacketEventBiConsumer(PacketEvent event, BiConsumer<PacketEvent, User> biConsumer)
    {
        final User user = User.safeGetUserFromPacketEvent(event);
        if (!User.isUserInvalid(user, module)) biConsumer.accept(event, user);
    }

    public PacketAdapter build()
    {
        Preconditions.checkArgument(this.onReceiving != null || this.onSending != null, "Tried to create PacketAdapter without receiving or sending actions.");

        if (this.onReceiving != null && this.onSending != null) {
            return new PacketAdapter(AntiCheatAddition.getInstance(), this.priority, this.types)
            {
                @Override
                public void onPacketReceiving(PacketEvent event)
                {
                    runPacketEventBiConsumer(event, onReceiving);
                }

                @Override
                public void onPacketSending(PacketEvent event)
                {
                    runPacketEventBiConsumer(event, onSending);
                }
            };
        } else if (onReceiving != null) {
            return new PacketAdapter(AntiCheatAddition.getInstance(), this.priority, this.types)
            {
                @Override
                public void onPacketReceiving(PacketEvent event)
                {
                    runPacketEventBiConsumer(event, onReceiving);
                }
            };
        } else {
            return new PacketAdapter(AntiCheatAddition.getInstance(), this.priority, this.types)
            {
                @Override
                public void onPacketSending(PacketEvent event)
                {
                    runPacketEventBiConsumer(event, onSending);
                }
            };
        }
    }
}