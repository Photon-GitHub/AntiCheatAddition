package de.photon.AACAdditionPro.util.clientsideentities.movement;

import de.photon.AACAdditionPro.util.clientsideentities.ClientsideEntity;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityHeadRotation;

public class HeadRotationManager
{
    /**
     * Moves the head to the desired position.
     */
    private void headMove(final ClientsideEntity clientsideEntity, final float yaw)
    {
        if (clientsideEntity.isSpawned()) {
            final WrapperPlayServerEntityHeadRotation headRotationWrapper = new WrapperPlayServerEntityHeadRotation();

            headRotationWrapper.setEntityID(clientsideEntity.getEntityID());
            headRotationWrapper.setHeadYaw(MathUtils.getFixRotation(yaw));

            headRotationWrapper.sendPacket(clientsideEntity.getObservedPlayer());
        }
    }
}
