package de.photon.aacadditionpro.api;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.additions.BrandHider;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import org.bukkit.entity.Player;

@SuppressWarnings({
                          "unused",
                          "WeakerAccess"
                  })
public final class AACAdditionProApi
{
    private AACAdditionProApi() {}

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
     * @param player     the player who should be looked up
     * @param moduleType the Check that should be looked up.
     *
     * @return The Violation-Level as an int.
     *
     * @throws IllegalArgumentException if the module does not have a {@link ViolationLevelManagement}.
     */
    public static int getVL(final Player player, final ModuleType moduleType)
    {
        return AACAdditionPro.getInstance().getModuleManager().getViolationLevelManagement(moduleType).getVL(player.getUniqueId());
    }

    /**
     * Used to set the ViolationLevel of a player.
     *
     * @param player     the player whose Violation-Level should be set.
     * @param moduleType the Check in which the Violation-Level will be set.
     * @param new_vl     The new Violation-Level of the player.
     *
     * @throws IllegalArgumentException if the module does not have a {@link ViolationLevelManagement}.
     */
    public static void setVl(final Player player, final ModuleType moduleType, final int new_vl)
    {
        AACAdditionPro.getInstance().getModuleManager().getViolationLevelManagement(moduleType).setVL(player, new_vl);
    }

    /**
     * Checks if a {@link Module} of a certain {@link ModuleType} is enabled.
     *
     * @param moduleType the {@link ModuleType} of the module that should be checked
     *
     * @return <code> true</code> if the module referred to by the {@link ModuleType} is enabled, else <code>false</code>
     */
    public static boolean getStateOfModule(final ModuleType moduleType)
    {
        return AACAdditionPro.getInstance().getModuleManager().getStateOfModule(moduleType);
    }

    /**
     * Enables or disables a module on the fly.
     *
     * @param moduleType the {@link ModuleType} of the module that should be enabled.
     */
    public static void setStateOfModule(final ModuleType moduleType, final boolean state)
    {
        AACAdditionPro.getInstance().getModuleManager().setStateOfModule(moduleType, state);
    }

    /**
     * Sets the brand that BrandHider should use
     */
    public static void setBrandHiderBrand(final String string)
    {
        BrandHider.setBrand(string);
    }
}
