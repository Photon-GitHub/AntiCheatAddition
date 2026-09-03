package de.photon.anticheataddition.modules.checks.packetfrequency;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
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
 */
public final class PacketFrequency extends ViolationModule
{
    public static final PacketFrequency INSTANCE = new PacketFrequency();

    private static final long EXPECTED_PACKET_TIME_NANOS = TimeUnit.MILLISECONDS.toNanos(50L);
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

        return ModuleLoader.of(this, PacketAdapterBuilder
                .of(this, PacketType.Play.Client.PLAYER_FLYING, PacketType.Play.Client.PLAYER_POSITION, PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION, PacketType.Play.Client.PLAYER_ROTATION)
                .priority(PacketListenerPriority.LOW)
                .onReceiving((event, user) -> {
                    final Timestamp timestamp = user.getTimeMap().at(TimeKey.PACKET_FREQUENCY);
                    if (timestamp.getTime() == 0L) {
                        timestamp.update();
                        return;
                    }

                    final long passedNanos = timestamp.passedNanos();
                    timestamp.update();

                    final ViolationCounter balance = user.getData().counter.packetFrequencyBalance;
                    final long currentBalance = balance.addWithMinimumAndGet(EXPECTED_PACKET_TIME_NANOS - passedNanos, minimumBalanceNanos);

                    Log.finest(() -> "PacketFrequency-Debug | Player: " + user.getPlayer().getName() + " | balance: " + currentBalance + "ns");

                    if (balance.greaterOrEqualToThreshold()) {
                        getManagement().flag(Flag.of(user)
                                                 .setAddedVl(VL_POLYNOMIAL.apply(currentBalance).intValue())
                                                 .setDebug(() -> "PacketFrequency-Debug | Player: " + user.getPlayer().getName() + " reached a packet balance of " + TimeUnit.NANOSECONDS.toMillis(currentBalance) + "ms."));
                    }

                })
                .build());
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(200, 1)
                                       .build();
    }
}
