package de.photon.anticheataddition.user.data;

import de.photon.anticheataddition.modules.checks.autopotion.AutoPotion;
import de.photon.anticheataddition.util.datastructure.statistics.DoubleStatistics;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Data
{
    public final Bool bool = new Bool();
    public final Floating floating = new Floating();
    public final Number number = new Number();
    public final Counter counter = new Counter();
    public final Object object = new Object();

    public static final class Bool
    {
        public volatile boolean allowedToJump = true;

        public boolean packetAnalysisAnimationExpected = false;
        public boolean packetAnalysisEqualRotationExpected = false;

        public volatile boolean movingUpwards = false;

        public volatile boolean sneaking = false;
        public volatile boolean sprinting = false;
    }

    public static final class Floating
    {
        public float autopotionBeforeLastSuddenPitch = 0F;
        public float autopotionBeforeLastSuddenYaw = 0F;

        public volatile float lastPacketPitch = -1F;
        public volatile float lastPacketYaw = -1F;
    }

    public static final class Number
    {
        public int dupingDoubleItemsDropped = 0;
        public int dupingDoubleItemsCollected = 0;

        public volatile int lastRawSlotClicked = 0;

        public volatile long lastSneakDuration = Long.MAX_VALUE;
        public volatile long lastSprintDuration = Long.MAX_VALUE;
    }

    /**
     * Counters to ignore a set amount of detections.
     * The default values were tuned by testing.
     */
    public static final class Counter
    {
        public final ViolationCounter inventoryAverageHeuristicsMisclicks = new ViolationCounter(0);
        public final ViolationCounter inventoryFrequencyFails = new ViolationCounter(40);
        public final ViolationCounter inventoryPerfectExitFails = new ViolationCounter(6);

        public final ViolationCounter packetAnalysisAimStepFails = new ViolationCounter(8);
        public final ViolationCounter packetAnalysisPerfectRotationYawFails = new ViolationCounter(5);
        public final ViolationCounter packetAnalysisPerfectRotationPitchFails = new ViolationCounter(5);

        public final ViolationCounter scaffoldAngleFails = new ViolationCounter(4);
        public final ViolationCounter scaffoldJumpingFails = new ViolationCounter(8);
        public final ViolationCounter scaffoldJumpingLegit = new ViolationCounter(18);
        public final ViolationCounter scaffoldRotationSignificantChangeFails = new ViolationCounter(3);
        public final ViolationCounter scaffoldRotationAngleSumFails = new ViolationCounter(3);
        public final ViolationCounter scaffoldRotationAngleVarianceFails = new ViolationCounter(10);
        public final ViolationCounter scaffoldRotationAngleSwitchFails = new ViolationCounter(4);

        public final ViolationCounter scaffoldSafewalkPositionFails = new ViolationCounter(4);
        public final ViolationCounter scaffoldSafewalkTimingFails = new ViolationCounter(3);
        public final ViolationCounter scaffoldSprintingFails = new ViolationCounter(5);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static final class Object
    {
        public final Map<TimeKey, DoubleStatistics> autoFishConsistencyData = Map.of(TimeKey.AUTOFISH_DETECTION, new DoubleStatistics(), TimeKey.AUTOFISH_AFK_DETECTION, new DoubleStatistics());
        public AutoPotion.AutoPotionState autoPotionState = AutoPotion.AutoPotionState.AWAIT_POTION_THROW;

        public Material dupingDoubleDroppedMaterial = Material.BEDROCK;

        // This contains all the locations currently in the check queue so that opening a chest twice does not cause a double vl.
        public final Set<Location> dupingSecretCacheCurrentlyCheckedLocations = ConcurrentHashMap.newKeySet();

        public ItemStack lastConsumedItemStack = null;
        public volatile Material lastMaterialClicked = Material.BEDROCK;
        public OptionalInt skinComponents = OptionalInt.empty();
    }
}
