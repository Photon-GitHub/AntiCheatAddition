package de.photon.anticheataddition.util.protocol;

import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.exception.UnknownMinecraftException;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class EntityMetadataIndex
{
    public static final int HEALTH;
    public static final int ARROWS_IN_ENTITY;
    public static final int SKIN_PARTS;

    static {
        switch (ServerVersion.ACTIVE) {
            case MC18 -> {
                HEALTH = 6;
                ARROWS_IN_ENTITY = 9;
                SKIN_PARTS = 10;
            }
            case MC112 -> {
                HEALTH = 7;
                ARROWS_IN_ENTITY = 10;
                SKIN_PARTS = 13;
            }
            case MC117, MC118, MC119, MC120, MC121  -> {
                HEALTH = 9;
                ARROWS_IN_ENTITY = 12;
                SKIN_PARTS = 17;
            }
            default -> throw new UnknownMinecraftException();
        }
    }
}
