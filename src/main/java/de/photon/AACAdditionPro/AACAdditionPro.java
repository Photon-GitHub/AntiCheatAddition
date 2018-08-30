package de.photon.AACAdditionPro;

import com.comphenix.protocol.ProtocolLibrary;
import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.api.killauraentity.KillauraEntityAddon;
import de.photon.AACAdditionPro.api.killauraentity.KillauraEntityController;
import de.photon.AACAdditionPro.command.MainCommand;
import de.photon.AACAdditionPro.events.APILoadedEvent;
import de.photon.AACAdditionPro.modules.ModuleManager;
import de.photon.AACAdditionPro.modules.additions.LogBot;
import de.photon.AACAdditionPro.modules.checks.AutoFish;
import de.photon.AACAdditionPro.modules.checks.AutoPotion;
import de.photon.AACAdditionPro.modules.checks.Esp;
import de.photon.AACAdditionPro.modules.checks.Fastswitch;
import de.photon.AACAdditionPro.modules.checks.GravitationalModifier;
import de.photon.AACAdditionPro.modules.checks.ImpossibleChat;
import de.photon.AACAdditionPro.modules.checks.InventoryHeuristics;
import de.photon.AACAdditionPro.modules.checks.InventoryHit;
import de.photon.AACAdditionPro.modules.checks.InventoryMove;
import de.photon.AACAdditionPro.modules.checks.InventoryRotation;
import de.photon.AACAdditionPro.modules.checks.KillauraEntity;
import de.photon.AACAdditionPro.modules.checks.MultiInteraction;
import de.photon.AACAdditionPro.modules.checks.Pingspoof;
import de.photon.AACAdditionPro.modules.checks.SkinBlinker;
import de.photon.AACAdditionPro.modules.checks.Teaming;
import de.photon.AACAdditionPro.modules.checks.Tower;
import de.photon.AACAdditionPro.modules.checks.packetanalysis.PacketAnalysis;
import de.photon.AACAdditionPro.modules.checks.scaffold.Scaffold;
import de.photon.AACAdditionPro.modules.clientcontrol.BetterSprintingControl;
import de.photon.AACAdditionPro.modules.clientcontrol.DamageIndicator;
import de.photon.AACAdditionPro.modules.clientcontrol.FiveZigControl;
import de.photon.AACAdditionPro.modules.clientcontrol.ForgeControl;
import de.photon.AACAdditionPro.modules.clientcontrol.LabyModControl;
import de.photon.AACAdditionPro.modules.clientcontrol.LiteLoaderControl;
import de.photon.AACAdditionPro.modules.clientcontrol.OldLabyModControl;
import de.photon.AACAdditionPro.modules.clientcontrol.PXModControl;
import de.photon.AACAdditionPro.modules.clientcontrol.SchematicaControl;
import de.photon.AACAdditionPro.modules.clientcontrol.VapeControl;
import de.photon.AACAdditionPro.modules.clientcontrol.VersionControl;
import de.photon.AACAdditionPro.modules.clientcontrol.WorldDownloaderControl;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.fakeentity.DelegatingKillauraEntityController;
import de.photon.AACAdditionPro.util.files.FileUtilities;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
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
    private static final transient String MINIMUM_AAC_VERSION = "3.5.0";

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
     * Checks if the given AAC version is too old for AACAdditionPro.
     */
    private static boolean isVersionTooOld(final String aacVersionNumber)
    {
        final String[] minimumVersionParts = AACAdditionPro.MINIMUM_AAC_VERSION.split("\\.");
        final String[] actualVersionParts = aacVersionNumber.split("\\.");

        final int smallestLength = Math.min(minimumVersionParts.length, actualVersionParts.length);
        int minimumVersionPart;
        int actualVersionPart;

        for (int i = 0; i < smallestLength; i++)
        {
            minimumVersionPart = Integer.valueOf(minimumVersionParts[i]);
            actualVersionPart = Integer.valueOf(actualVersionParts[i]);

            if (minimumVersionPart == actualVersionPart)
                continue;

            return minimumVersionPart > actualVersionPart;
        }

        // Same numbers, now check for additions
        return minimumVersionParts.length > actualVersionParts.length;
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
            if (isVersionTooOld(this.getServer().getPluginManager().getPlugin("AAC").getDescription().getVersion()))
            {
                VerboseSender.getInstance().sendVerboseMessage("AAC version is not supported.", true, true);
                VerboseSender.getInstance().sendVerboseMessage("This plugin needs AAC version " + MINIMUM_AAC_VERSION + " or newer.", true, true);
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
            this.moduleManager = new ModuleManager(ImmutableSet.of(
                    // Additions
                    new LogBot(),

                    // ClientControl
                    new BetterSprintingControl(),
                    new DamageIndicator(),
                    new FiveZigControl(),
                    new ForgeControl(),
                    new LabyModControl(),
                    new LiteLoaderControl(),
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
                    new Tower())
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

        // Remove all the Listeners, PacketListeners
        ProtocolLibrary.getProtocolManager().removePacketListeners(this);
        HandlerList.unregisterAll(AACAdditionPro.getInstance());

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
     * Disables all addons regarding the {@link KillauraEntity} check.
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
