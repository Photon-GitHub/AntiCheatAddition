package de.photon.aacadditionpro.api;

import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleManager;
import de.photon.aacadditionpro.modules.additions.BrandHider;
import de.photon.aacadditionpro.util.pluginmessage.labymod.LabyModProtocol;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import org.bukkit.entity.Player;

import java.util.UUID;

@SuppressWarnings({
                          "unused",
                          "WeakerAccess"
                  })
public final class AACAdditionProApi
{
    private AACAdditionProApi() {}

    /**
     * Used to get the ViolationLevel of a player.
     *
     * @param uuid     the {@link UUID} of the player who should be looked up
     * @param moduleId the check that should be looked up.
     *
     * @return The Violation-Level as an int.
     *
     * @throws NullPointerException if the module does not exist or does not have a {@link ViolationManagement}.
     */
    public static int getVL(final UUID uuid, final String moduleId)
    {
        return ModuleManager.getViolationModuleMap().getModule(moduleId).getManagement().getVL(uuid);
    }

    /**
     * Used to set the ViolationLevel of a player.
     *
     * @param player   the player whose Violation-Level should be set.
     * @param moduleId the check in which the Violation-Level will be set.
     * @param new_vl   The new Violation-Level of the player.
     *
     * @throws NullPointerException if the module does not exist or does not have a {@link ViolationManagement}.
     */
    public static void setVl(final Player player, final String moduleId, final int new_vl)
    {
        ModuleManager.getViolationModuleMap().getModule(moduleId).getManagement().setVL(player, new_vl);
    }

    /**
     * Checks if a {@link Module} is enabled.
     *
     * @param moduleId the id of the module that should be checked
     *
     * @return <code>true</code> if the module referred to by the id is enabled, else <code>false</code>
     *
     * @throws NullPointerException if there is no module with the given moduleId.
     */
    public static boolean getStateOfModule(final String moduleId)
    {
        return ModuleManager.getModuleMap().getModule(moduleId).isEnabled();
    }

    /**
     * Enables or disables a {@link Module}.
     *
     * @param moduleId the id of the module that should be enabled or disabled.
     * @param enabled  <code>true</code> to enable the module, <code>false</code> to disable.
     *
     * @throws NullPointerException if there is no module with the given moduleId.
     */
    public static void setStateOfModule(final String moduleId, final boolean enabled)
    {
        ModuleManager.getModuleMap().getModule(moduleId).setEnabled(enabled);
    }

    /**
     * Sets the brand that BrandHider should use
     */
    public static void setBrandHiderBrand(final String string)
    {
        BrandHider.setBrand(string);
    }


    /**
     * Manually send a tablist server banner to a LabyMod client.
     * It is your responsibility to ensure that the client is indeed a LabyMod client.
     *
     * @param player   LabyMod player
     * @param imageUrl the url of the image that shall be displayed on the client's tablist
     */
    public static void sendServerBanner(Player player, String imageUrl)
    {
        LabyModProtocol.sendServerBanner(player, imageUrl);
    }
}
