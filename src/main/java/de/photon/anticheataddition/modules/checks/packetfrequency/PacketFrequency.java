package de.photon.anticheataddition.modules.checks.packetfrequency;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.user.data.Timestamp;
import de.photon.anticheataddition.user.data.ViolationCounter;
import de.photon.anticheataddition.util.log.Log;
import de.photon.anticheataddition.util.mathematics.Polynomial;
import de.photon.anticheataddition.util.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

import java.util.concurrent.TimeUnit;

/**
 * Tracks the packet-time balance of each player to detect clients that send movement packets too quickly.
 * <p>
 * It tracks both the position or flying packets and the end tick packets in separate balances, as modern Minecraft versions
 * allow the client to send no position packet in a tick.
 * <p>
 * Furthermore, it compares the two balances to ensure that the two packet balances are synchronous to catch partial timer cheats.
 */
public final class PacketFrequency extends ViolationModule
{
    public static final PacketFrequency INSTANCE = new PacketFrequency();

    private static final long EXPECTED_PACKET_TIME_NANOS = TimeUnit.MILLISECONDS.toNanos(50L);
    private static final long MAXIMUM_BALANCE_DIFFERENCE_NANOS = TimeUnit.SECONDS.toNanos(1L);
    private static final Polynomial VL_POLYNOMIAL = new Polynomial(1e-7, 1);

    private final long minimumBalanceNanos;

    private PacketFrequency()
    {
        super("PacketFrequency");

        // The configuration expresses the floor as a positive, human-readable millisecond amount.
        final long minimumBalanceMillis = Math.max(0L, loadLong(".minimum_balance", 10_000L));
        this.minimumBalanceNanos = -TimeUnit.MILLISECONDS.toNanos(minimumBalanceMillis);
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        final ModuleLoader.Builder builder = ModuleLoader
                .builder(this)
                .addPacketListeners(
                        PacketAdapterBuilder
                                .of(this, PacketType.Play.Client.PLAYER_FLYING, PacketType.Play.Client.PLAYER_POSITION, PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION, PacketType.Play.Client.PLAYER_ROTATION)
                                .priority(PacketListenerPriority.LOW)
                                .onReceivingRaw(this::resetIfBypassed)
                                .onReceiving((event, user) -> updateBalance(user, TimeKey.PACKET_FREQUENCY, user.getData().counter.packetFrequencyBalance, "position"))
                                .build());

        // The end tick packet first appeared in Minecraft 1.21.2 -> 1.21.5 as the latest support release.
        if (ServerVersion.MC121_5.activeIsLaterOrEqual()) {
            builder.addPacketListeners(
                    PacketAdapterBuilder
                            .of(this, PacketType.Play.Client.CLIENT_TICK_END)
                            .priority(PacketListenerPriority.LOW)
                            .onReceivingRaw(this::resetIfBypassed)
                            .onReceiving((event, user) -> {
                                if (updateBalance(user, TimeKey.PACKET_FREQUENCY_END_TICK, user.getData().counter.packetFrequencyEndTickBalance, "end tick")) compareBalances(user);
                            })
                            .build());
        }

        return builder.build();
    }

    /**
     * Bypassed users are filtered out by {@link PacketAdapterBuilder}, so their timestamps would otherwise become
     * stale. Reset the complete measurement state while the bypass is active so that removing the bypass starts a
     * fresh measurement window instead of treating the bypass duration as delayed packets.
     */
    private void resetIfBypassed(PacketReceiveEvent event)
    {
        final User user = User.getUser(event);
        if (user != null && User.isUserInvalid(user, this)) resetState(user);
    }

    static void resetState(User user)
    {
        user.getTimeMap().at(TimeKey.PACKET_FREQUENCY).setToZero();
        user.getTimeMap().at(TimeKey.PACKET_FREQUENCY_END_TICK).setToZero();
        user.getData().counter.packetFrequencyBalance.setToZero();
        user.getData().counter.packetFrequencyEndTickBalance.setToZero();
    }

    /**
     * Updates and validates the time balance for one packet stream.
     *
     * @return whether the balance was updated, rather than initialized
     */
    private boolean updateBalance(User user, TimeKey timeKey, ViolationCounter balance, String balanceName)
    {
        final Timestamp timestamp = user.getTimeMap().at(timeKey);
        if (timestamp.getTime() == 0L) {
            timestamp.update();
            return false;
        }

        final long passedNanos = timestamp.passedNanos();
        timestamp.update();

        final long currentBalance = balance.addWithMinimumAndGet(EXPECTED_PACKET_TIME_NANOS - passedNanos, minimumBalanceNanos);

        Log.finest(() -> "PacketFrequency-Debug | Player: " + user.getPlayer().getName() + " | " + balanceName + " balance: " + currentBalance + "ns");

        if (balance.greaterOrEqualToThreshold()) {
            getManagement().flag(Flag.of(user)
                                     .setAddedVl(VL_POLYNOMIAL.apply(currentBalance).intValue())
                                     .setDebug(() -> "PacketFrequency-Debug | Player: " + user.getPlayer().getName() + " reached a " + balanceName + " packet balance of " + TimeUnit.NANOSECONDS.toMillis(currentBalance) + "ms."));
        }
        return true;
    }

    /**
     * A client can send at most one position packet per tick, so its position balance must not lead its tick-end balance.
     */
    private void compareBalances(User user)
    {
        final long positionBalance = user.getData().counter.packetFrequencyBalance.getCounter();
        final long endTickBalance = user.getData().counter.packetFrequencyEndTickBalance.getCounter();
        final long positionEndTickBalance = positionBalance - endTickBalance;

        Log.finest(() -> "PacketFrequency-Debug | Player: " + user.getPlayer().getName() + " | position/end tick balance: " + positionEndTickBalance + "ns");

        if (positionEndTickBalance >= MAXIMUM_BALANCE_DIFFERENCE_NANOS) {
            getManagement().flag(Flag.of(user)
                                     .setAddedVl(VL_POLYNOMIAL.apply(positionEndTickBalance).intValue())
                                     .setDebug(() -> "PacketFrequency-Debug | Player: " + user.getPlayer().getName() + " exceeded the end tick balance by " + TimeUnit.NANOSECONDS.toMillis(positionEndTickBalance) + "ms."));
        }
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .loadThresholdsToManagement()
                                       .withDecay(200, 1)
                                       .build();
    }
}
