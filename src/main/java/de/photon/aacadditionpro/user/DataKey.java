package de.photon.aacadditionpro.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Getter
@RequiredArgsConstructor
public enum DataKey
{
    AUTOFISH_FAIL_COUNTER(Long.class, 0),

    LAST_CONSUMED_ITEM_STACK(ItemStack.class, null),

    /**
     * The last slot a person clicked.<br>
     * This variable is used to prevent false positives based on spam-clicking one slot.
     */
    LAST_MATERIAL_CLICKED(Material.class, Material.BEDROCK),
    LAST_RAW_SLOT_CLICKED(Integer.class, 0),

    PACKET_ANALYSIS_COMPARE_FAILS(Long.class, 0),
    PACKET_ANALYSIS_LAST_ANIMATION_EXPECTED(Boolean.class, false),
    PACKET_ANALYSIS_LAST_EQUAL_ROTATION_EXPECTED(Boolean.class, false),
    PACKET_ANALYSIS_LAST_POSITION_FORCE_LOCATION(Location.class, null),
    PACKET_ANALYSIS_REAL_LAST_YAW(Float.class, -1),
    PACKET_ANALYSIS_REAL_LAST_PITCH(Float.class, -1),

    SCAFFOLD_ANGLE_FAILS(Long.class, 0),
    SCAFFOLD_ROTATION_FAILS(Long.class, 0),
    SCAFFOLD_SAFEWALK_1_FAILS(Long.class, 0),
    SCAFFOLD_SAFEWALK_2_FAILS(Long.class, 0),
    SCAFFOLD_SPRINTING_FAILS(Long.class, 0),

    SKIN_COMPONENTS(Integer.class, null);

    private final Class clazz;
    private final Object defaultValue;
}
