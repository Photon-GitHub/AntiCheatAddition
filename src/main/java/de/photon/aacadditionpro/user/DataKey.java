package de.photon.aacadditionpro.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Getter
@RequiredArgsConstructor
public enum DataKey
{
    AUTOFISH_FAIL_COUNTER(Long.class),

    LAST_CONSUMED_ITEM_STACK(ItemStack.class),

    /**
     * The last slot a person clicked.<br>
     * This variable is used to prevent false positives based on spam-clicking one slot.
     */
    LAST_MATERIAL_CLICKED(Material.class),
    LAST_RAW_SLOT_CLICKED(Integer.class),

    PACKET_ANALYSIS_COMPARE_FAILS(Long.class),
    PACKET_ANALYSIS_REAL_LAST_YAW(Float.class),
    PACKET_ANALYSIS_REAL_LAST_PITCH(Float.class),

    SCAFFOLD_ANGLE_FAILS(Long.class),
    SCAFFOLD_ROTATION_FAILS(Long.class),
    SCAFFOLD_SAFEWALK_1_FAILS(Long.class),
    SCAFFOLD_SAFEWALK_2_FAILS(Long.class),
    SCAFFOLD_SPRINTING_FAILS(Long.class),

    SKIN_COMPONENTS(Long.class);

    private final Class clazz;
}
