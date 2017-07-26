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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

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
    private static final String minimumAACVersion = "3.1.5";

    private static final Field killauraEntityControllerField;
    private static final Field delegatingKillauraEntityControllerField;

    static {
        killauraEntityControllerField = KillauraEntityAddon.class.getDeclaredFields()[0];
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
            //                                         Get server version                                             //
            // ------------------------------------------------------------------------------------------------------ //

            // Minecraft-Version
            final String versionOutput = this.getServer().getVersion();
            for (final ServerVersion serverVersion : ServerVersion.values()) {
                if (versionOutput.contains(serverVersion.getVersionOutputString())) {
                    this.serverVersion = serverVersion;
                    break;
                }
            }

            // ------------------------------------------------------------------------------------------------------ //
            //                                    Not supported server version                                        //
            // ------------------------------------------------------------------------------------------------------ //

            if (this.serverVersion == null) {
                VerboseSender.sendVerboseMessage("Server version is not supported.", true, true);

                // StringBuilder for the message explaining what versions are supported
                final StringBuilder supportedMessage = new StringBuilder(44);
                supportedMessage.append("Supported versions:");

                // Automatically get all the supported versions and append them to the message
                for (final ServerVersion serverVersion : ServerVersion.values()) {
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

            final String aacVersion = this.getServer().getPluginManager().getPlugin("AAC").getDescription().getVersion();
            final int aacVersionInt = Integer.valueOf(aacVersion.replace(".", ""));
            final int minAACVersionInt = Integer.valueOf(minimumAACVersion.replace(".", ""));

            if (aacVersionInt < minAACVersionInt) {
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

            // AdditionManager
            AdditionManager.startAdditionManager();

            // CheckManager
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

    public void setKillauraEntityAddon(KillauraEntityAddon killauraEntityAddon)
    {
        // Check input
        if (killauraEntityAddon == null) {
            throw new IllegalArgumentException("EXTERNAL PLUGIN ERROR: killauraEntityAddon is null");
        }
        // Check provided plugin (Required for better exception messages)
        JavaPlugin plugin = killauraEntityAddon.getPlugin();
        if (plugin == null || plugin.getName() == null) {
            throw new IllegalArgumentException("EXTERNAL PLUGIN ERROR: Invalid plugin argument for killauraEntityAddon given: " + plugin);
        } else if (plugin.getDescription() == null || plugin.getDescription().getName() == null || plugin.getName().equalsIgnoreCase(AACAdditionPro.getInstance().getName())) {
            throw new IllegalArgumentException("EXTERNAL PLUGIN ERROR: Invalid plugin argument for killauraEntityAddon given: " + plugin.getName() + " - " + plugin);
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

    public void disableKillauraEntityAPI()
    {
        if (this.killauraEntityAddon != null) {
            try {
                killauraEntityControllerField.set(this.killauraEntityAddon, null);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        if (this.currentDelegatingKillauraEntityController != null) {
            try {
                delegatingKillauraEntityControllerField.set(currentDelegatingKillauraEntityController, null);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
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
