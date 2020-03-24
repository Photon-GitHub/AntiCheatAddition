package de.photon.aacadditionpro.user;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum DataKey
{
    AUTOEAT_TIMEOUT(true),

    AUTOFISH_DETECTION(true),
    AUTOFISH_FAIL_COUNTER(false),

    AUTOPOTION_DETECTION(true),
    AUTOPOTION_LAST_SUDDEN_PITCH(true),
    AUTOPOTION_LAST_SUDDEN_YAW(true),
    AUTOPOTION_TIMEOUT(true),

    INVENTORY_OPENED(true),

    LAST_COMBAT(true),
    LAST_CONSUME_EVENT(true),
    LAST_FISH_BITE(true),
    LAST_HEAD_MOVEMENT(true),
    LAST_INVENTORY_CLICK(true),
    LAST_RESPAWN(true),
    LAST_RIGHT_CLICK_CONSUMABLE_ITEM_EVENT(true),
    LAST_RIGHT_CLICK_EVENT(true),
    LAST_SLOT_CLICKED(true),
    LAST_SNEAK(true),
    LAST_SNEAK_DURATION(true),
    LAST_SPRINT(true),
    LAST_SPRINT_DURATION(true),
    LAST_TELEPORT(true),
    LAST_VELOCITY_CHANGE(true),
    LAST_WORLD_CHANGE(true),
    LAST_XYZ_MOVEMENT(true),
    LAST_XZ_MOVEMENT(true),

    LOGIN_TIME(true),

    PACKET_ANALYSIS_COMPARE_FAILS(false),
    PACKET_ANALYSIS_LAST_COMPARE_FLAG(true),

    PINGSPOOF_DETECTION(true),

    SCAFFOLD_ANGLE_FAILS(false),
    SCAFFOLD_ROTATION_FAILS(false),
    SCAFFOLD_SAFEWALK_1_FAILS(false),
    SCAFFOLD_SAFEWALK_2_FAILS(false),
    SCAFFOLD_SPRINTING_FAILS(false),

    SKIN_COMPONENTS(false),

    TOWER_TIMEOUT(true);

    final boolean isTimeStamp;
}
