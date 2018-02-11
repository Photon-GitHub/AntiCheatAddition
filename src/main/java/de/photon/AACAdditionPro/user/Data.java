package de.photon.AACAdditionPro.user;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * Basic class to save data on a per - {@link User} basis.
 */
public abstract class Data
{
    @Getter(AccessLevel.PROTECTED)
    private User user;

    public Data(User user)
    {
        this.user = user;
    }

    /**
     * Unregisters this {@link Data} to prepare its deletion.
     */
    public void unregister()
    {
        this.user = null;
    }
}
