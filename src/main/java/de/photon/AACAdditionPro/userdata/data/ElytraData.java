package de.photon.AACAdditionPro.userdata.data;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.userdata.Data;
import de.photon.AACAdditionPro.userdata.User;

public class ElytraData extends Data
{
    public ElytraData(final User theUser)
    {
        super(false, theUser);
    }

    /**
     * Used to determine if someone is currently flying with an Elytra.
     * This is version-safe
     *
     * @return false when the person is not flying with an Elytra or the server version does not support Elytras.
     */
    public boolean isNotFlyingWithElytra()
    {
        switch (AACAdditionPro.getInstance().getServerVersion())
        {
            case MC188:
                return true;
            case MC110:
            case MC111:
                return !theUser.getPlayer().isGliding();
        }
        return true;
    }
}
