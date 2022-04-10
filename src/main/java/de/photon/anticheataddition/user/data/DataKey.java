package de.photon.anticheataddition.user.data;

import com.google.common.base.Preconditions;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.util.datastructure.statistics.DoubleStatistics;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalInt;
import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataKey
{
    @Getter
    @AllArgsConstructor
    public enum Bool
    {
        ALLOWED_TO_JUMP(true),
        AUTOPOTION_ALREADY_THROWN(false),
        PACKET_ANALYSIS_ANIMATION_EXPECTED(false),
        PACKET_ANALYSIS_EQUAL_ROTATION_EXPECTED(false),
        POSITIVE_VELOCITY(false),
        SNEAKING(false),
        SPRINTING(false);

        private final boolean defaultValue;
    }

    @Getter
    @AllArgsConstructor
    public enum Int
    {
        LAST_RAW_SLOT_CLICKED(0);

        private final int defaultValue;
    }

    @Getter
    @AllArgsConstructor
    public enum Long
    {
        LAST_SNEAK_DURATION(java.lang.Long.MAX_VALUE),
        LAST_SPRINT_DURATION(java.lang.Long.MAX_VALUE),

        PACKET_ANALYSIS_COMPARE_FAILS(0L);

        private final long defaultValue;
    }

    @Getter
    @AllArgsConstructor
    public enum Float
    {
        AUTOPOTION_LAST_SUDDEN_PITCH(0F),
        AUTOPOTION_LAST_SUDDEN_YAW(0F),

        LAST_PACKET_PITCH(-1F),
        LAST_PACKET_YAW(-1F);

        private final float defaultValue;
    }

    @Getter
    @AllArgsConstructor
    public enum Double
    {
        ;
        private final double defaultValue;
    }

    public enum Count
    {
        AUTOFISH_FAILED("AutoFish.parts.consistency.maximum_fails"),

        INVENTORY_AVERAGE_HEURISTICS_MISCLICKS(0),
        INVENTORY_PERFECT_EXIT_FAILS("Inventory.parts.PerfectExit.violation_threshold"),

        SCAFFOLD_ANGLE_FAILS("Scaffold.parts.Angle.violation_threshold"),
        SCAFFOLD_JUMPING_FAILS("Scaffold.parts.Jumping.violation_threshold"),
        SCAFFOLD_JUMPING_LEGIT(20),
        SCAFFOLD_ROTATION_FAILS("Scaffold.parts.Rotation.violation_threshold"),
        SCAFFOLD_SAFEWALK_POSITION_FAILS("Scaffold.parts.Safewalk.Position.violation_threshold"),
        SCAFFOLD_SAFEWALK_TIMING_FAILS("Scaffold.parts.Safewalk.Timing.violation_threshold"),
        SCAFFOLD_SPRINTING_FAILS("Scaffold.parts.Sprinting.violation_threshold");

        private final long threshold;

        Count(long threshold)
        {
            this.threshold = threshold;
        }

        Count(String configPath)
        {
            Preconditions.checkArgument(AntiCheatAddition.getInstance().getConfig().contains(configPath), "Tried to load ViolationCounter from nonexistent path " + configPath);
            this.threshold = AntiCheatAddition.getInstance().getConfig().getLong(configPath);
        }

        public ViolationCounter createDefaultCounter()
        {
            return new ViolationCounter(this.threshold);
        }
    }

    @AllArgsConstructor
    public enum Obj
    {
        AUTOFISH_CONSISTENCY_DATA(DoubleStatistics.class, DoubleStatistics::new),

        LAST_CONSUMED_ITEM_STACK(ItemStack.class, () -> null),

        LAST_MATERIAL_CLICKED(Material.class, () -> Material.BEDROCK),

        PACKET_ANALYSIS_LAST_POSITION_FORCE_LOCATION(Location.class, () -> null),

        SKIN_COMPONENTS(OptionalInt.class, OptionalInt::empty);

        @Getter private final Class<?> clazz;
        @NotNull private final Supplier<Object> createDefaultObject;

        public Object createDefaultObject()
        {
            return createDefaultObject.get();
        }
    }
}
