package de.photon.aacadditionpro.modules;

import de.photon.aacadditionpro.AACAdditionPro;

public interface RestrictedBungeecord
{
    /**
     * Checks if the server is running on bungeecord.
     *
     * @return <code>true</code> if the server is not running on bungeecord, else <code>false<code/>.
     */
    static boolean allowedToStart()
    {
        return !AACAdditionPro.getInstance().isBungeecord();
    }
}
