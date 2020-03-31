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
    ALLOWED_TO_JUMP(Boolean.class, true),

    LAST_CONSUMED_ITEM_STACK(ItemStack.class, null),

    /**
     * The last slot a person clicked.<br>
     * This variable is used to prevent false positives based on spam-clicking one slot.
     */
    LAST_MATERIAL_CLICKED(Material.class, Material.BEDROCK),
    LAST_RAW_SLOT_CLICKED(Integer.class, 0),

    LAST_SNEAK_DURATION(Long.class, Long.MAX_VALUE),
    LAST_SPRINT_DURATION(Long.class, Long.MAX_VALUE),

    PACKET_ANALYSIS_ANIMATION_EXPECTED(Boolean.class, false),
    PACKET_ANALYSIS_COMPARE_FAILS(Long.class, 0),
    PACKET_ANALYSIS_EQUAL_ROTATION_EXPECTED(Boolean.class, false),
    PACKET_ANALYSIS_LAST_POSITION_FORCE_LOCATION(Location.class, null),
    PACKET_ANALYSIS_REAL_LAST_PITCH(Float.class, -1),
    PACKET_ANALYSIS_REAL_LAST_YAW(Float.class, -1),

    POSITIVE_VELOCITY(Boolean.class, false),

    SKIN_COMPONENTS(Integer.class, null),

    SNEAKING(Boolean.class, false),
    SPRINTING(Boolean.class, false);

    private final Class clazz;
    private final Object defaultValue;
}
