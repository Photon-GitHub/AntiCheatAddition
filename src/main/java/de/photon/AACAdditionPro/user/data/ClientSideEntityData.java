package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.user.TimeData;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.util.fakeentity.ClientsidePlayerEntity;

public class ClientSideEntityData extends TimeData
{
    public ClientsidePlayerEntity clientSidePlayerEntity;

    public byte respawnTrys = 0;

    public ClientSideEntityData(final User user)
    {
        super(user, 0L);
    }

    /**
     * Despawns the {@link ClientsidePlayerEntity} of this data if it exists.
     */
    public void despawnClientSidePlayerEntity()
    {
        if (this.clientSidePlayerEntity != null)
        {
            this.clientSidePlayerEntity.despawn();
            this.clientSidePlayerEntity = null;
        }
    }

    @Override
    public void unregister()
    {
        this.despawnClientSidePlayerEntity();
        super.unregister();
    }
}
