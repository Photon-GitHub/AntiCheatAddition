package de.photon.aacadditionpro.user.data;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.util.datastructure.statistics.DoubleStatistics;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataKey
{
    @Getter
    @AllArgsConstructor
    public enum BooleanKey
    {
        ALLOWED_TO_JUMP(true),
        AUTOPOTION_ALREADY_THROWN(false),
        PACKET_ANALYSIS_ANIMATION_EXPECTED(false),
        PACKET_ANALYSIS_EQUAL_ROTATION_EXPECTED(false),
        POSITIVE_VELOCITY(false),
        SNEAKING(false),
        SPRINTING(false);

        private final Boolean defaultValue;
    }

    @Getter
    @AllArgsConstructor
    public enum IntegerKey
    {
        LAST_RAW_SLOT_CLICKED(0),
        SKIN_COMPONENTS(null);

        private final Integer defaultValue;
    }

    @Getter
    @AllArgsConstructor
    public enum LongKey
    {
        LAST_SNEAK_DURATION(Long.MAX_VALUE),
        LAST_SPRINT_DURATION(Long.MAX_VALUE),

        PACKET_ANALYSIS_COMPARE_FAILS(0L);

        private final Long defaultValue;
    }

    @Getter
    @AllArgsConstructor
    public enum FloatKey
    {
        AUTOPOTION_LAST_SUDDEN_PITCH(0F),
        AUTOPOTION_LAST_SUDDEN_YAW(0F),
        LAST_PACKET_PITCH(-1F),
        LAST_PACKET_YAW(-1F);

        private final Float defaultValue;
    }

    @Getter
    @AllArgsConstructor
    public enum DoubleKey
    {
        ;
        private final Double defaultValue;
    }

    @Getter
    public enum CounterKey
    {
        AUTOFISH_FAILED("AutoFish.parts.consistency.maximum_fails"),
        INVENTORY_AVERAGE_HEURISTICS_MISCLICKS(0),
        INVENTORY_PERFECT_EXIT_FAILS("Inventory.parts.PerfectExit.violation_threshold"),
        SCAFFOLD_ANGLE_FAILS("Scaffold.parts.Angle.violation_threshold"),
        SCAFFOLD_ROTATION_FAILS("Scaffold.parts.Rotation.violation_threshold"),
        SCAFFOLD_SAFEWALK_POSITION_FAILS("Scaffold.parts.Safewalk.type1.violation_threshold"),
        SCAFFOLD_SAFEWALK_TIMING_FAILS("Scaffold.parts.Safewalk.type2.violation_threshold"),
        SCAFFOLD_SPRINTING_FAILS("Scaffold.parts.Sprinting.violation_threshold");

        private final ViolationCounter defaultValue;

        CounterKey(long defaultValue)
        {
            this.defaultValue = new ViolationCounter(defaultValue);
        }

        CounterKey(String configPath)
        {
            Preconditions.checkArgument(AACAdditionPro.getInstance().getConfig().contains(configPath), "Tried to load ViolationCounter from nonexistant path " + configPath);
            this.defaultValue = new ViolationCounter(AACAdditionPro.getInstance().getConfig().getLong(configPath));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum ObjectKey
    {
        AUTOFISH_CONSISTENCY_DATA(DoubleStatistics.class, new DoubleStatistics()),

        LAST_CONSUMED_ITEM_STACK(ItemStack.class, null),

        /**
         * The last slot a person clicked.<br>
         * This variable is used to prevent false positives based on spam-clicking one slot.
         */
        LAST_MATERIAL_CLICKED(Material.class, Material.BEDROCK),


        PACKET_ANALYSIS_LAST_POSITION_FORCE_LOCATION(Location.class, null);

        private final Class clazz;
        private final Object defaultValue;
    }
}
