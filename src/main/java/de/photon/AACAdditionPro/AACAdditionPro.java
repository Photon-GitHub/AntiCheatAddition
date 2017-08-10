package de.photon.AACAdditionPro;

import com.comphenix.protocol.ProtocolLibrary;
import de.photon.AACAdditionPro.addition.AdditionManager;
import de.photon.AACAdditionPro.api.KillauraEntityAddon;
import de.photon.AACAdditionPro.api.KillauraEntityController;
import de.photon.AACAdditionPro.checks.CheckManager;
import de.photon.AACAdditionPro.command.MainCommand;
import de.photon.AACAdditionPro.events.APILoadedEvent;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.entities.DelegatingKillauraEntityController;
import de.photon.AACAdditionPro.util.files.FileUtilities;
import de.photon.AACAdditionPro.util.multiversion.ServerVersion;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Objects;

public class AACAdditionPro extends JavaPlugin
{
    private ServerVersion serverVersion = null;

    /**
     * Indicates if the loading process is completed.
     */
    private boolean loaded = false;

    /**
     * The minimum AAC version required to run the plugin.
     * If the version of AAC is older than this version the plugin will disable itself in order to assure that bugs
     * cannot be caused by an incompatible AAC version.
     */
    private static final transient String minimumAACVersion = "3.1.5";

    private static final Field killauraEntityControllerField;
    private static final Field delegatingKillauraEntityControllerField;

    static {
        killauraEntityControllerField = KillauraEntityAddon.class.getDeclaredFields()[1];
        killauraEntityControllerField.setAccessible(true);

        delegatingKillauraEntityControllerField = DelegatingKillauraEntityController.class.getDeclaredFields()[0];
        delegatingKillauraEntityControllerField.setAccessible(true);
    }

    @Getter
    private KillauraEntityAddon killauraEntityAddon;

    private KillauraEntityController currentDelegatingKillauraEntityController;

    @Setter
    private KillauraEntityController killauraEntityController;


    /**
     * This will get the object of the plugin registered on the server.
     *
     * @return the active instance of this plugin on the server.
     */
    public static AACAdditionPro getInstance()
    {
        return AACAdditionPro.getPlugin(AACAdditionPro.class);
    }

    /**
     * Parses an int from a version {@link String} that has numbers and dots in it.
     *
     * @param versionString the {@link String} from which the version int should be parsed.
     *
     * @return the int representation of all numbers in the {@link String} with the dots filtered out.
     */
    private static int getVersionNumber(final String versionString)
    {
        return Integer.valueOf(versionString.replace(".", ""));
    }

    /**
     * Registers a new {@link Listener} for AACAdditionPro.
     *
     * @param listener the {@link Listener} which should be registered in the {@link org.bukkit.plugin.PluginManager}
     */
    public void registerListener(final Listener listener)
    {
        this.getServer().getPluginManager().registerEvents(listener, this);
    }

    @Override
    public FileConfiguration getConfig()
    {
        File savedFile = null;
        try {
            savedFile = FileUtilities.saveFileInFolder("config.yml");
        } catch (final IOException e) {
            e.printStackTrace();
        }

        if (savedFile == null) {
            throw new NullPointerException("Config file needed to get the FileConfiguration was not found.");
        }
        return YamlConfiguration.loadConfiguration(savedFile);
    }

    @Override
    public void onEnable()
    {
        try {
            // Enabled message
            VerboseSender.sendVerboseMessage("Enabling plugin...", true, false);

            // ------------------------------------------------------------------------------------------------------ //
            //                                      Validate server version                                           //
            // ------------------------------------------------------------------------------------------------------ //

            try {
                this.serverVersion = ServerVersion.getServerVersion();
            }

            // ------------------------------------------------------------------------------------------------------ //
            //                                      Unsupported server version                                        //
            // ------------------------------------------------------------------------------------------------------ //
            catch (IllegalArgumentException exception) {
                VerboseSender.sendVerboseMessage("Server version is not supported.", true, true);

                final ServerVersion[] supportedVersions = ServerVersion.values();

                /*
                    StringBuilder for the message explaining what versions are supported
                    19 for "Supported versions:"
                    5 for every entry (5 is the max.)
                    2 for " " and "," per entry
                    -----------------------------------------
                    -> 19 + 7 * ServerVersion.values().length
                */
                final StringBuilder supportedMessage = new StringBuilder(19 + supportedVersions.length * 7);
                supportedMessage.append("Supported versions:");

                // Automatically get all the supported versions and append them to the message
                for (final ServerVersion serverVersion : supportedVersions) {
                    supportedMessage.append(" ");
                    supportedMessage.append(serverVersion.getVersionOutputString());
                    supportedMessage.append(",");
                }

                // Delete the last comma
                supportedMessage.deleteCharAt(supportedMessage.length() - 1);

                // Print the complete message
                VerboseSender.sendVerboseMessage(supportedMessage.toString(), true, true);
                return;
            }

            // ------------------------------------------------------------------------------------------------------ //
            //                                       Not supported AAC version                                        //
            // ------------------------------------------------------------------------------------------------------ //

            // Is the numerical representation of the min AAC version smaller than the representation of the real version
            if (getVersionNumber(minimumAACVersion) > getVersionNumber(Bukkit.getPluginManager().getPlugin("AAC").getDescription().getVersion())) {
                VerboseSender.sendVerboseMessage("AAC version is not supported.", true, true);
                VerboseSender.sendVerboseMessage("This plugin needs AAC version " + minimumAACVersion + " or newer.", true, true);
                return;
            }

            // ------------------------------------------------------------------------------------------------------ //
            //                                                 Config                                                 //
            // ------------------------------------------------------------------------------------------------------ //

            getConfig().options().copyDefaults(true);
            FileUtilities.saveFileInFolder("config.yml");

            // ------------------------------------------------------------------------------------------------------ //
            //                                                Features                                                //
            // ------------------------------------------------------------------------------------------------------ //

            // UserManager
            this.registerListener(new UserManager());

            // Managers
            AdditionManager.startAdditionManager();
            CheckManager.startCheckManager();

            // Commands
            this.getCommand(MainCommand.instance.commandName).setExecutor(MainCommand.instance);

            // ------------------------------------------------------------------------------------------------------ //
            //                                          Enabled-Verbose + API                                         //
            // ------------------------------------------------------------------------------------------------------ //
            this.getLogger().info(this.getName() + " Version " + this.getDescription().getVersion() + " enabled");

            // API loading finished
            this.loaded = true;
            this.getServer().getPluginManager().callEvent(new APILoadedEvent());

        } catch (final Exception e) {
            // ------------------------------------------------------------------------------------------------------ //
            //                                              Failed loading                                            //
            // ------------------------------------------------------------------------------------------------------ //
            this.getLogger().severe("Loading failed:");
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable()
    {
        // Plugin is already disabled -> VerboseSender is not allowed to register a task
        VerboseSender.setAllowedToRegisterTasks(false);

        // Disable all checks
        AdditionManager.additionManagerInstance.forEach(Module::disable);
        CheckManager.checkManagerInstance.forEach(Module::disable);

        // Remove all the Listeners, PacketListeners
        ProtocolLibrary.getProtocolManager().removePacketListeners(this);

        VerboseSender.sendVerboseMessage("AACAdditionPro disabled.", true, false);
        VerboseSender.sendVerboseMessage(" ", true, false);

        // Task scheduling
        loaded = false;
    }

    /**
     * Activates an KillauraEntityAddon
     *
     * @param killauraEntityAddon the {@link KillauraEntityAddon} that should be activated and possibly override the current one.
     */
    public void setKillauraEntityAddon(KillauraEntityAddon killauraEntityAddon)
    {
        // Make sure that the provided KillauraEntityAddon is not null and
        // check provided plugin (Required for better exception messages)
        JavaPlugin plugin = Objects.requireNonNull(killauraEntityAddon, "EXTERNAL PLUGIN ERROR: KillauraEntityAddon is null").getPlugin();

        if (plugin == null || plugin.getName() == null) {
            throw new IllegalArgumentException("EXTERNAL PLUGIN ERROR: Invalid plugin provided as KillauraEntityAddon: " + plugin);
        }

        // Invalid description
        if (plugin.getDescription() == null ||
            plugin.getDescription().getName() == null ||
            // AACAdditionPro itself cannot be a KillauraEntityAddon
            plugin.getName().equalsIgnoreCase(AACAdditionPro.getInstance().getName()))
        {
            throw new IllegalArgumentException("EXTERNAL PLUGIN ERROR: Invalid plugin provided as KillauraEntityAddon: " + plugin.getName() + " - " + plugin);
        }

        // Check KillauraEntity-API being available
        if (killauraEntityController == null) {
            throw new IllegalStateException("KillauraEntity-API not ready, enable the KillauraEntity check");
        }

        // Set up the new KillauraEntityAddon
        this.killauraEntityAddon = killauraEntityAddon;
        currentDelegatingKillauraEntityController = new DelegatingKillauraEntityController(killauraEntityController);

        try {
            killauraEntityControllerField.set(this.killauraEntityAddon, currentDelegatingKillauraEntityController);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Disables all addons regarding the {@link de.photon.AACAdditionPro.checks.subchecks.KillauraEntity} check.
     */
    public void disableKillauraEntityAPI()
    {
        try {
            if (this.killauraEntityAddon != null) {
                killauraEntityControllerField.set(this.killauraEntityAddon, null);
            }

            if (this.currentDelegatingKillauraEntityController != null) {
                delegatingKillauraEntityControllerField.set(currentDelegatingKillauraEntityController, null);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the {@link ServerVersion} of the server the plugin is running on.
     */
    public ServerVersion getServerVersion()
    {
        return serverVersion;
    }

    /**
     * @return true if the loading process is completed and false if not.
     */
    public boolean isLoaded()
    {
        return loaded;
    }
}
