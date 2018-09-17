package de.photon.AACAdditionPro.util.fakeentity;

import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.reflection.Reflect;

import java.lang.reflect.Field;

public final class EntityIdUtil
{
    private static final Field entityCountField;

    static
    {
        entityCountField = Reflect.fromNMS("Entity").field("entityCount").getField();
        entityCountField.setAccessible(true);
    }

    /**
     * This gets the next free EntityID and increases the entityCount field accordingly.
     * Prevents bypasses based on the EntityID, especially for higher numbers
     *
     * @return the next free EntityID
     */
    public static int getNextEntityID()
    {
        try
        {
            // Get entity id for next entity (this one)
            final int entityID = entityCountField.getInt(null);

            // Increase entity id for next entity
            entityCountField.setInt(null, entityID + 1);
            return entityID;
        } catch (IllegalAccessException e)
        {
            VerboseSender.getInstance().sendVerboseMessage("Unable to get a valid entity id.", true, true);
            e.printStackTrace();
            return 1000000;
        }
    }
}