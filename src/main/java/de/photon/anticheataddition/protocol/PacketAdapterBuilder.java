package de.photon.anticheataddition.protocol;

import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
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
    @NotNull private final Set<PacketTypeCommon> types;

    private PacketListenerPriority priority = PacketListenerPriority.NORMAL;
    private BiConsumer<PacketReceiveEvent, User> onReceiving = null;
    private BiConsumer<PacketSendEvent, User> onSending = null;

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

    public static PacketAdapterBuilder of(@NotNull Module module, @NotNull PacketTypeCommon... types)
    {
        return of(module, Set.of(types));
    }

    public static PacketAdapterBuilder of(@NotNull Module module, @NotNull Set<PacketTypeCommon> types)
    {
        Preconditions.checkNotNull(types, "Tried to create PacketAdapterBuilder with null types.");
        Preconditions.checkArgument(!types.isEmpty(), "Tried to create PacketAdapterBuilder without types.");
        return new PacketAdapterBuilder(module, types);
    }

    public PacketAdapterBuilder priority(PacketListenerPriority priority)
    {
        Preconditions.checkNotNull(priority, "Tried to set PacketAdapterBuilder ListenerPriority to null.");
        this.priority = priority;
        return this;
    }

    public PacketAdapterBuilder onReceiving(BiConsumer<PacketReceiveEvent, User> onReceiving)
    {
        this.onReceiving = onReceiving;
        return this;
    }

    public PacketAdapterBuilder onSending(BiConsumer<PacketSendEvent, User> onSending)
    {
        this.onSending = onSending;
        return this;
    }

    private void runPacketReceiveEventBiConsumer(PacketReceiveEvent event, BiConsumer<PacketReceiveEvent, User> biConsumer)
    {
        final User user = User.safeGetUserFromPacketEvent(event);
        if (!User.isUserInvalid(user, module)) biConsumer.accept(event, user);
    }

    private void runPacketSendEventBiConsumer(PacketSendEvent event, BiConsumer<PacketSendEvent, User> biConsumer)
    {
        final User user = User.safeGetUserFromPacketEvent(event);
        if (!User.isUserInvalid(user, module)) biConsumer.accept(event, user);
    }

    public PacketListenerCommon build()
    {
        Preconditions.checkArgument(this.onReceiving != null || this.onSending != null, "Tried to create PacketAdapter without receiving or sending actions.");

        if (this.onReceiving != null && this.onSending != null) {
            return new PacketListenerAbstract(this.priority)
            {
                @Override
                public void onPacketReceive(PacketReceiveEvent event)
                {
                    if (types.contains(event.getPacketType()))
                        runPacketReceiveEventBiConsumer(event, onReceiving);
                }

                @Override
                public void onPacketSend(PacketSendEvent event)
                {
                    if (types.contains(event.getPacketType()))
                        runPacketSendEventBiConsumer(event, onSending);
                }
            };
        } else if (onReceiving != null) {
            return new PacketListenerAbstract(this.priority)
            {
                @Override
                public void onPacketReceive(PacketReceiveEvent event)
                {
                    if (types.contains(event.getPacketType()))
                        runPacketReceiveEventBiConsumer(event, onReceiving);
                }
            };
        } else {
            return new PacketListenerAbstract(this.priority)
            {
                @Override
                public void onPacketSend(PacketSendEvent event)
                {
                    if (types.contains(event.getPacketType()))
                        runPacketSendEventBiConsumer(event, onSending);
                }
            };
        }
    }
}