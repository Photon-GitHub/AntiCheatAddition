package de.photon.anticheataddition.protocol;

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
            case MC18:
                HEALTH = 6;
                ARROWS_IN_ENTITY = 9;
                SKIN_PARTS = 10;
                break;
            case MC112:
                HEALTH = 7;
                ARROWS_IN_ENTITY = 10;
                SKIN_PARTS = 13;
                break;
            case MC115:
            case MC116:
                HEALTH = 8;
                ARROWS_IN_ENTITY = 11;
                SKIN_PARTS = 16;
                break;
            case MC117:
            case MC118:
                HEALTH = 9;
                ARROWS_IN_ENTITY = 12;
                SKIN_PARTS = 17;
                break;
            default:
                throw new UnknownMinecraftException();
        }
    }
}
