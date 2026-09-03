package de.photon.anticheataddition.modules.checks.packetfrequency;

import de.photon.anticheataddition.Dummy;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import de.photon.anticheataddition.util.violationlevels.threshold.Threshold;
import de.photon.anticheataddition.util.violationlevels.threshold.ThresholdManagement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

class PacketFrequencyTest
{
    @BeforeAll
    static void setup()
    {
        Dummy.mockAntiCheatAddition();
    }

    @Test
    void bypassResetClearsMeasurementState()
    {
        final User user = Dummy.mockUser();
        user.getTimeMap().at(TimeKey.PACKET_FREQUENCY).update();
        user.getTimeMap().at(TimeKey.PACKET_FREQUENCY_END_TICK).update();
        user.getData().counter.packetFrequencyBalance.addAndGet(2_000_000_000L);
        user.getData().counter.packetFrequencyEndTickBalance.addAndGet(2_000_000_000L);

        PacketFrequency.resetState(user);

        Assertions.assertEquals(0L, user.getTimeMap().at(TimeKey.PACKET_FREQUENCY).getTime());
        Assertions.assertEquals(0L, user.getTimeMap().at(TimeKey.PACKET_FREQUENCY_END_TICK).getTime());
        Assertions.assertEquals(0L, user.getData().counter.packetFrequencyBalance.getCounter());
        Assertions.assertEquals(0L, user.getData().counter.packetFrequencyEndTickBalance.getCounter());
    }

    @Test
    void configuredThresholdsAreLoaded()
            throws ReflectiveOperationException
    {
        final Field thresholdsField = ViolationManagement.class.getDeclaredField("thresholds");
        thresholdsField.setAccessible(true);
        final ThresholdManagement thresholds = (ThresholdManagement) thresholdsField.get(PacketFrequency.INSTANCE.getManagement());
        final List<Threshold> configuredThresholds = thresholds.getThresholds();

        Assertions.assertEquals(List.of(110, 160, 200), configuredThresholds.stream().map(Threshold::vl).toList());
    }
}
