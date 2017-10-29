package de.photon.AACAdditionPro.userdata.data;

import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.util.entities.ClientsidePlayerEntity;

public class ClientSideEntityData extends TimeData
{
    public ClientsidePlayerEntity clientSidePlayerEntity;

    public byte respawnTrys = 0;

    public ClientSideEntityData(final User theUser)
    {
        super(false, theUser);
    }
}
