package de.photon.anticheataddition.user.data;

import de.photon.anticheataddition.util.datastructure.buffer.RingBuffer;
import de.photon.anticheataddition.util.datastructure.statistics.DoubleStatistics;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.OptionalInt;

public final class Data
{
    public final Bool bool = new Bool();
    public final Floating floating = new Floating();
    public final Number number = new Number();
    public final Counter counter = new Counter();
    public final Object object = new Object();

    @Getter
    @Setter
    public static final class Bool
    {
        private boolean allowedToJump = true;

        private boolean autopotionAlreadyThrown = false;

        private boolean packetAnalysisAnimationExpected = false;
        private boolean packetAnalysisEqualRotationExpected = false;

        private boolean positiveVelocity = false;

        private boolean sneaking = false;
        private boolean sprinting = false;
    }

    @Getter
    @Setter
    public static final class Floating
    {
        private float autopotionLastSuddenPitch = 0F;
        private float autopotionLastSuddenYaw = 0F;

        private volatile float lastPacketPitch = -1F;
        private volatile float lastPacketYaw = -1F;
    }

    @Getter
    @Setter
    public static final class Number
    {
        private int lastRawSlotClicked = 0;

        private volatile long lastSneakDuration = Long.MAX_VALUE;
        private volatile long lastSprintDuration = Long.MAX_VALUE;
    }

    @Getter
    public static final class Counter
    {
        private final ViolationCounter autofishFailed = new ViolationCounter("AutoFish.parts.consistency.maximum_fails");

        private final ViolationCounter inventoryAverageHeuristicsMisclicks = new ViolationCounter(0);
        private final ViolationCounter inventoryPerfectExitFails = new ViolationCounter("Inventory.parts.PerfectExit.violation_threshold");

        private final ViolationCounter scaffoldAngleFails = new ViolationCounter("Scaffold.parts.Angle.violation_threshold");
        private final ViolationCounter scaffoldJumpingFails = new ViolationCounter("Scaffold.parts.Jumping.violation_threshold");
        private final ViolationCounter scaffoldJumpingLegit = new ViolationCounter(20);
        private final ViolationCounter scaffoldRotationFails = new ViolationCounter("Scaffold.parts.Rotation.violation_threshold");
        private final ViolationCounter scaffoldSafewalkPositionFails = new ViolationCounter("Scaffold.parts.Safewalk.Position.violation_threshold");
        private final ViolationCounter scaffoldSafewalkTimingFails = new ViolationCounter("Scaffold.parts.Safewalk.Timing.violation_threshold");
        private final ViolationCounter scaffoldSprintingFails = new ViolationCounter("Scaffold.parts.Sprinting.violation_threshold");
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Getter
    @Setter
    public static final class Object
    {
        private final DoubleStatistics autoFishConsistencyData = new DoubleStatistics();
        private ItemStack lastConsumedItemStack = null;
        private final RingBuffer<ItemStack> lastDroppedStacks = new RingBuffer<>(10);
        private volatile Material lastMaterialClicked = Material.BEDROCK;
        private OptionalInt skinComponents = OptionalInt.empty();
    }
}
