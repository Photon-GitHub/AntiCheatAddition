package de.photon.AACAdditionPro;

import com.comphenix.protocol.ProtocolLibrary;
import de.photon.AACAdditionPro.additions.LogBot;
import de.photon.AACAdditionPro.additions.PerHeuristicCommands;
import de.photon.AACAdditionPro.api.killauraentity.KillauraEntityAddon;
import de.photon.AACAdditionPro.api.killauraentity.KillauraEntityController;
import de.photon.AACAdditionPro.checks.subchecks.AutoFish;
import de.photon.AACAdditionPro.checks.subchecks.AutoPotion;
import de.photon.AACAdditionPro.checks.subchecks.Esp;
import de.photon.AACAdditionPro.checks.subchecks.Fastswitch;
import de.photon.AACAdditionPro.checks.subchecks.GravitationalModifier;
import de.photon.AACAdditionPro.checks.subchecks.ImpossibleChat;
import de.photon.AACAdditionPro.checks.subchecks.InventoryHeuristics;
import de.photon.AACAdditionPro.checks.subchecks.InventoryHit;
import de.photon.AACAdditionPro.checks.subchecks.InventoryMove;
import de.photon.AACAdditionPro.checks.subchecks.InventoryRotation;
import de.photon.AACAdditionPro.checks.subchecks.KillauraEntity;
import de.photon.AACAdditionPro.checks.subchecks.MultiInteraction;
import de.photon.AACAdditionPro.checks.subchecks.PacketAnalysis;
import de.photon.AACAdditionPro.checks.subchecks.Pingspoof;
import de.photon.AACAdditionPro.checks.subchecks.Scaffold;
import de.photon.AACAdditionPro.checks.subchecks.SkinBlinker;
import de.photon.AACAdditionPro.checks.subchecks.Teaming;
import de.photon.AACAdditionPro.checks.subchecks.Tower;
import de.photon.AACAdditionPro.checks.subchecks.clientcontrol.BetterSprintingControl;
import de.photon.AACAdditionPro.checks.subchecks.clientcontrol.DamageIndicator;
import de.photon.AACAdditionPro.checks.subchecks.clientcontrol.FiveZigControl;
import de.photon.AACAdditionPro.checks.subchecks.clientcontrol.ForgeControl;
import de.photon.AACAdditionPro.checks.subchecks.clientcontrol.LabyModControl;
import de.photon.AACAdditionPro.checks.subchecks.clientcontrol.LiteloaderControl;
import de.photon.AACAdditionPro.checks.subchecks.clientcontrol.OldLabyModControl;
import de.photon.AACAdditionPro.checks.subchecks.clientcontrol.PXModControl;
import de.photon.AACAdditionPro.checks.subchecks.clientcontrol.SchematicaControl;
import de.photon.AACAdditionPro.checks.subchecks.clientcontrol.VapeControl;
import de.photon.AACAdditionPro.checks.subchecks.clientcontrol.VersionControl;
import de.photon.AACAdditionPro.checks.subchecks.clientcontrol.WorldDownloaderControl;
import de.photon.AACAdditionPro.command.MainCommand;
import de.photon.AACAdditionPro.events.APILoadedEvent;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.fakeentity.DelegatingKillauraEntityController;
import de.photon.AACAdditionPro.util.files.FileUtilities;
import de.photon.AACAdditionPro.util.multiversion.ServerVersion;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.ViaAPI;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;

public class AACAdditionPro extends JavaPlugin
{
    /**
     * Indicates if the loading process is completed.
     */
    @Getter
    private boolean loaded = false;

    /**
     * The minimum AAC version required to run the plugin.
     * If the version of AAC is older than this version the plugin will disable itself in order to assure that bugs
     * cannot be caused by an incompatible AAC version.
     */
    private static final transient String minimumAACVersion = "3.3.11";

    private static final Field killauraEntityControllerField;
    private static final Field delegatingKillauraEntityControllerField;

    static
    {
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

    // Cache the config for better performance
    private FileConfiguration cachedConfig;

    @Getter
    private ModuleManager moduleManager;

    @Getter
    private ViaAPI<Player> viaAPI;

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
     * Checks if the version number is greater than the minimum version number.
     */
    private static boolean isVersionValid(String versionNumber)
    {
        final String[] minVersionParts = AACAdditionPro.minimumAACVersion.split(".");
        final String[] versionNumberParts = versionNumber.split(".");

        final int minLength = Math.min(minVersionParts.length, versionNumberParts.length);
        int oneNumber;
        int twoNumber;

        for (int i = 0; i < minLength; i++)
        {
            oneNumber = Integer.valueOf(minVersionParts[i]);
            twoNumber = Integer.valueOf(versionNumberParts[i]);

            if (oneNumber > twoNumber)
            {
                return false;
            }
            else if (oneNumber < twoNumber)
            {
                return true;
            }
        }

        // Same numbers, now check for additions
        return minVersionParts.length <= versionNumberParts.length;
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
        if (cachedConfig == null)
        {
            File savedFile = null;
            try
            {
                savedFile = FileUtilities.saveFileInFolder("config.yml");
            } catch (final IOException e)
            {
                VerboseSender.getInstance().sendVerboseMessage("Failed to create config folder / file", true, true);
                e.printStackTrace();
            }
            cachedConfig = YamlConfiguration.loadConfiguration(Objects.requireNonNull(savedFile, "Config file needed to get the FileConfiguration was not found."));
        }

        return cachedConfig;
    }

    @Override
    public void onEnable()
    {
        try
        {
            // Enabled message
            VerboseSender.getInstance().sendVerboseMessage("Enabling plugin...", true, false);

            // ------------------------------------------------------------------------------------------------------ //
            //                                      Unsupported server version                                        //
            // ------------------------------------------------------------------------------------------------------ //
            if (ServerVersion.getActiveServerVersion() == null ||
                // Unsupported
                !ServerVersion.getActiveServerVersion().isSupported())
            {
                VerboseSender.getInstance().sendVerboseMessage("Server version is not supported.", true, true);

                // Print the complete message
                VerboseSender.getInstance().sendVerboseMessage(
                        "Supported versions:" +
                        String.join(
                                // Versions should be divided by commas.
                                ", ",
                                // Create a List of all the possible server versions
                                Arrays.stream(ServerVersion.values()).filter(ServerVersion::isSupported).map(ServerVersion::getVersionOutputString).toArray(String[]::new)),
                        true, true);
                return;
            }

            // ------------------------------------------------------------------------------------------------------ //
            //                                       Not supported AAC version                                        //
            // ------------------------------------------------------------------------------------------------------ //

            // Is the numerical representation of the min AAC version smaller than the representation of the real version
            if (!isVersionValid(this.getServer().getPluginManager().getPlugin("AAC").getDescription().getVersion()))
            {
                VerboseSender.getInstance().sendVerboseMessage("AAC version is not supported.", true, true);
                VerboseSender.getInstance().sendVerboseMessage("This plugin needs AAC version " + minimumAACVersion + " or newer.", true, true);
                return;
            }

            // The first getConfig call will automatically saveToFile and cache the config.

            // ------------------------------------------------------------------------------------------------------ //
            //                                              Plugin hooks                                              //
            // ------------------------------------------------------------------------------------------------------ //

            // Call is correct here as Bukkit always has a player api.
            if (this.getServer().getPluginManager().getPlugin("ViaVersion") != null)
            {
                //noinspection unchecked
                viaAPI = Via.getAPI();
            }

            // ------------------------------------------------------------------------------------------------------ //
            //                                                Features                                                //
            // ------------------------------------------------------------------------------------------------------ //

            // Managers
            this.registerListener(new UserManager());
            this.moduleManager = new ModuleManager(
                    // Additions
                    new LogBot(),
                    new PerHeuristicCommands(),

                    // ClientControl
                    new BetterSprintingControl(),
                    new DamageIndicator(),
                    new FiveZigControl(),
                    new ForgeControl(),
                    new LabyModControl(),
                    new LiteloaderControl(),
                    new OldLabyModControl(),
                    new PXModControl(),
                    new SchematicaControl(),
                    new VapeControl(),
                    new VersionControl(),
                    new WorldDownloaderControl(),

                    // Normal checks
                    new AutoFish(),
                    new AutoPotion(),
                    new Esp(),
                    new Fastswitch(),
                    new GravitationalModifier(),
                    new ImpossibleChat(),
                    new InventoryHeuristics(),
                    new InventoryHit(),
                    new InventoryMove(),
                    new InventoryRotation(),
                    new KillauraEntity(),
                    new MultiInteraction(),
                    new PacketAnalysis(),
                    new Pingspoof(),
                    new Scaffold(),
                    new SkinBlinker(),
                    new Teaming(),
                    new Tower()
            );

            // Commands
            this.getCommand(MainCommand.instance.name).setExecutor(MainCommand.instance);

            // ------------------------------------------------------------------------------------------------------ //
            //                                          Enabled-Verbose + API                                         //
            // ------------------------------------------------------------------------------------------------------ //
            this.getLogger().info(this.getName() + " Version " + this.getDescription().getVersion() + " enabled");

            // API loading finished
            this.loaded = true;
            this.getServer().getPluginManager().callEvent(new APILoadedEvent());

        } catch (final Exception e)
        {
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
        VerboseSender.getInstance().setAllowedToRegisterTasks(false);

        // Disable all checks
        try
        {
            moduleManager.forEach(Module::disable);
        } catch (NullPointerException ignore)
        {
            // This can happen if the modulemanager is already finalized.
        }

        // Remove all the Listeners, PacketListeners
        ProtocolLibrary.getProtocolManager().removePacketListeners(this);

        VerboseSender.getInstance().sendVerboseMessage("AACAdditionPro disabled.", true, false);
        VerboseSender.getInstance().sendVerboseMessage(" ", true, false);

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

        if (plugin == null || plugin.getName() == null)
        {
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

        // Set up the new KillauraEntityAddon
        this.killauraEntityAddon = killauraEntityAddon;
        currentDelegatingKillauraEntityController = new DelegatingKillauraEntityController(
                // Check KillauraEntity-API being available
                Objects.requireNonNull(killauraEntityController, "KillauraEntity-API not ready, enable the KillauraEntity check"));

        try
        {
            killauraEntityControllerField.set(this.killauraEntityAddon, currentDelegatingKillauraEntityController);
        } catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Disables all addons regarding the {@link de.photon.AACAdditionPro.checks.subchecks.KillauraEntity} check.
     */
    public void disableKillauraEntityAPI()
    {
        try
        {
            if (this.killauraEntityAddon != null)
            {
                killauraEntityControllerField.set(this.killauraEntityAddon, null);
            }

            if (this.currentDelegatingKillauraEntityController != null)
            {
                delegatingKillauraEntityControllerField.set(currentDelegatingKillauraEntityController, null);
            }
        } catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }
}
