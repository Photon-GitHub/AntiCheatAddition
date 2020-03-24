package de.photon.aacadditionpro.olduser;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * Basic class to save data on a per - {@link UserOld} basis.
 */
public abstract class DataOld
{
    @Getter(AccessLevel.PROTECTED)
    private UserOld user;

    public DataOld(UserOld user)
    {
        this.user = user;
    }

    /**
     * Unregisters this {@link DataOld} to prepare its deletion.
     */
    public void unregister()
    {
        this.user = null;
    }
}
