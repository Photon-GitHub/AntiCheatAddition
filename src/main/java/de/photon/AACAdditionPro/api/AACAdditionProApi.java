package de.photon.AACAdditionPro.api;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.CheckManager;
import de.photon.AACAdditionPro.exceptions.NoViolationLevelManagementException;
import org.bukkit.entity.Player;

@SuppressWarnings({
        "unused",
        "WeakerAccess"
})
public final class AACAdditionProApi
{
    /**
     * This should be used prior to any operation with the api.
     *
     * @return true if the plugin is loaded and false if it is not loaded.
     */
    public static boolean isLoaded()
    {
        return AACAdditionPro.getInstance().isLoaded();
    }

    /**
     * Used to get the ViolationLevel of a player.
     *
     * @param player           the player who should be looked up
     * @param additionHackType the Check that should be looked up.
     *
     * @return The Violation-Level as an int.
     *
     * @throws NoViolationLevelManagementException if the check of the given {@link AdditionHackType} does not have violation-levels
     */
    public static int getVL(final Player player, final AdditionHackType additionHackType) throws NoViolationLevelManagementException
    {
        return CheckManager.checkManagerInstance.getCheck(additionHackType).getViolationLevelManagement().getVL(player.getUniqueId());
    }

    /**
     * Used to set the ViolationLevel of a player.
     *
     * @param player           the player whose Violation-Level should be set.
     * @param additionHackType the Check in which the Violation-Level will be set.
     * @param new_vl           The new Violation-Level of the player.
     *
     * @throws NoViolationLevelManagementException if the check of the given {@link AdditionHackType} does not have violation-levels
     */
    public static void setVl(final Player player, final AdditionHackType additionHackType, final int new_vl) throws NoViolationLevelManagementException
    {
        CheckManager.checkManagerInstance.getCheck(additionHackType).getViolationLevelManagement().setVL(player, new_vl);
    }

    /**
     * This enables or disables a check live.
     *
     * @param additionHackType the {@link AdditionHackType} of the check that should be enabled.
     */
    public static void setStateOfCheck(final AdditionHackType additionHackType, final boolean state)
    {
        CheckManager.checkManagerInstance.setStateOfCheck(additionHackType, state);
    }

    /**
     * Sets an optional {@link KillauraEntityAddon} to improve and customize your KillauraEntity check.
     * The {@link KillauraEntityAddon} which has been set most recently is the active one.
     */
    public static void setKillauraEntityAddon(KillauraEntityAddon addon)
    {
        AACAdditionPro.getInstance().setKillauraEntityAddon(addon);
    }
}
