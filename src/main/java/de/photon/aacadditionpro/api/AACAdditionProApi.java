package de.photon.aacadditionpro.api;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ModuleManager;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.modules.additions.BrandHider;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.pluginmessage.labymod.LabyModProtocol;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import lombok.val;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

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
    public static int getVL(@NotNull final UUID uuid, @NotNull final String moduleId)
    {
        Preconditions.checkNotNull(uuid, "Tried to get vl of null uuid.");
        Preconditions.checkNotNull(moduleId, "Tried to get vl of null moduleId.");
        return Preconditions.checkNotNull(ModuleManager.getViolationModuleMap().getModule(moduleId), "The moduleId does not exist or the associated module has no vl management.").getManagement().getVL(uuid);
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
        Preconditions.checkNotNull(player, "Tried to set vl of null player.");
        Preconditions.checkNotNull(moduleId, "Tried to set vl of null moduleId.");
        Preconditions.checkNotNull(ModuleManager.getViolationModuleMap().getModule(moduleId), "The moduleId does not exist or the associated module has no vl management.").getManagement().setVL(player, new_vl);
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
        Preconditions.checkNotNull(moduleId, "Tried to get state of null moduleId.");
        val module = ModuleManager.getModuleMap().getModule(moduleId);
        Preconditions.checkNotNull(module, "Tried to get state of unknown module.");
        return module.isEnabled();
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
        Preconditions.checkNotNull(moduleId, "Tried to set state of null moduleId.");
        val module = ModuleManager.getModuleMap().getModule(moduleId);
        Preconditions.checkNotNull(module, "Tried to set state of unknown module.");
        module.setEnabled(enabled);
    }

    /**
     * Sets the brand that BrandHider should use
     */
    public static void setBrandHiderBrand(final String string)
    {
        Preconditions.checkNotNull(string, "Tried to set BrandHider brand to null.");
        BrandHider.INSTANCE.setBrand(string);
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
        Preconditions.checkNotNull(player, "Tried to send server banner to null player.");
        Preconditions.checkNotNull(imageUrl, "Tried to send null server banner.");
        LabyModProtocol.sendServerBanner(player, imageUrl);
    }

    /**
     * This allows you to create your own modules, while leaving all the loading to AACAdditionPro.
     * PLEASE NOTE THAT YOU MIGHT USE PRIVATE APIS THAT MAY CHANGE BETWEEN VERSIONS.
     * You can even add your own values to the config to AACAdditionPro and load them.
     * <p>
     * <p>
     * <p>
     * An in-depth tutorial is provided on the overview page of spigot, this is just the quick-start:
     * <p>1) Extend either {@link Module} or {@link ViolationModule}, depending on whether or not your Module is supposed to have a vls (ViolationModule) or not (Module)</p>
     * <p>If you know what you are doing you can also use {@link de.photon.aacadditionpro.events.SentinelEvent} to create your own complex Sentinel checks.</p>
     * <p>2) Supply the config string and add your module to the config of AACAdditionPro. Don't forget to add enabled.</p>
     * <p>
     * <p>
     * <p>
     * It should now look something like this (see other modules):
     * <p>ExampleModule:</p>
     * <p>enabled: true</p>
     * <p>
     * <p>
     * <p>
     * 2.1) If you want additional config values, use the {@link LoadFromConfiguration} annotation.
     * Possible config example:
     * <p>ExampleModule:</p>
     * <p>enabled: true</p>
     * <p>myBooleanValue: true</p>
     * <p>myIntValue: 42</p>
     * <p>
     * <p>
     * <p>
     * 3) Implement the {@link ModuleLoader}-creation method from {@link Module}.
     * If your module is a {@link Listener} it will automatically be registered, no need to call the method of {@link ModuleLoader}, any other
     * {@link Listener} need to be registered that way. For any further information, please check the provided methods.
     * <p>
     * <p>
     * <p>
     * 4) To flag a player in a {@link ViolationModule}, use this.getManagement().flag(Flag.of(event.getPlayer()).setAddedVl(vl);
     * For further actions, like cancelling please check out the {@link Flag} documentation.
     * <p>
     * <p>
     * <p>
     * 5) Call this method with your module and start your server.
     */
    public static void addExternalModule(Module module)
    {
        Preconditions.checkNotNull(module, "Tried to add null module");
        Preconditions.checkArgument(ModuleManager.getModuleMap().getModule(module.getModuleId()) == null, "Tried to add a module with an already existing moduleId.");
        ModuleManager.addExternalModule(module);
    }
}
