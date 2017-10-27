package de.photon.AACAdditionPro.userdata.data;

import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.util.entities.ClientsidePlayerEntity;

public class ClientSideEntityData extends TimeData
{
    public ClientsidePlayerEntity clientSidePlayerEntity;

    public boolean spawnedOnDemand = false;

    public ClientSideEntityData(final User theUser)
    {
        super(false, theUser);
    }
}
