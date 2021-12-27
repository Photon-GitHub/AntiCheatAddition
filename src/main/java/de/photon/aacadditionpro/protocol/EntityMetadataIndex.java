package de.photon.aacadditionpro.protocol;

import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.exception.UnknownMinecraftException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EntityMetadataIndex
{
    public static final int HEALTH;
    public static final int ARROWS_IN_ENTITY;
    public static final int SKIN_PARTS;


    static {
        // Passenger problems
        switch (ServerVersion.getActiveServerVersion()) {
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
                // index 9 in 1.17+
                HEALTH = 9;
                ARROWS_IN_ENTITY = 12;
                SKIN_PARTS = 17;
                break;
            default:
                throw new UnknownMinecraftException();
        }
    }
}
