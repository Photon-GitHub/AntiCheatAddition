package de.photon.anticheataddition;

import com.comphenix.protocol.ProtocolLibrary;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.ViaAPI;
import de.photon.anticheataddition.commands.MainCommand;
import de.photon.anticheataddition.modules.ModuleManager;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.DataUpdaterEvents;
import de.photon.anticheataddition.util.config.Configs;
import de.photon.anticheataddition.util.messaging.DebugSender;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.apache.commons.lang.StringUtils;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.api.FloodgateApi;

import java.io.File;
import java.util.Locale;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Getter
public class AntiCheatAddition extends JavaPlugin
{
    /**
     * The expected players to be on the entire server across all worlds.
     */
    public static final int SERVER_EXPECTED_PLAYERS = 150;

    /**
     * The expected player to be in a world.
     */
    public static final int WORLD_EXPECTED_PLAYERS = 50;

    private static final int BSTATS_PLUGIN_ID = 14608;


    @Setter(AccessLevel.PROTECTED)
    @Getter private static AntiCheatAddition instance;

    @Getter(lazy = true) private final FileConfiguration config = generateConfig();
    private ViaAPI<?> viaAPI;
    private FloodgateApi floodgateApi;

    private boolean bungeecord = false;

    /**
     * Registers a new {@link Listener} for AntiCheatAddition.
     *
     * @param listener the {@link Listener} which should be registered in the {@link org.bukkit.plugin.PluginManager}
     */
    public void registerListener(final Listener listener)
    {
        this.getServer().getPluginManager().registerEvents(listener, this);
    }

    private FileConfiguration generateConfig()
    {
        // This will already write an error if the config could not be saved.
        this.saveDefaultConfig();
        return YamlConfiguration.loadConfiguration(new File(this.getDataFolder(), "config.yml"));
    }

    private static boolean checkForPlugin(String pluginName, Metrics metrics)
    {
        final boolean enabled = Bukkit.getServer().getPluginManager().isPluginEnabled(pluginName);
        DebugSender.INSTANCE.sendDebug(StringUtils.capitalize(pluginName) + (enabled ? "hooked" : "not found"), true, false);
        metrics.addCustomChart(new SimplePie(pluginName.toLowerCase(Locale.ROOT), enabled ? () -> "Yes" : () -> "No"));
        return enabled;
    }

    @Override
    public void onEnable()
    {
        try {
            // Now needs to be done via this ugly way as the original way caused a loading error.
            setInstance(this);

            // ------------------------------------------------------------------------------------------------------ //
            //                                      Unsupported server version                                        //
            // ------------------------------------------------------------------------------------------------------ //
            if (!ServerVersion.ACTIVE.isSupported()) {
                DebugSender.INSTANCE.sendDebug("Server version is not supported.", true, true);
                DebugSender.INSTANCE.sendDebug("Supported versions: " + ServerVersion.ALL_SUPPORTED_VERSIONS.stream().map(ServerVersion::getVersionOutputString).collect(Collectors.joining(", ")),
                                               true, true);
                return;
            }

            // ------------------------------------------------------------------------------------------------------ //
            //                                               Bungeecord                                               //
            // ------------------------------------------------------------------------------------------------------ //

            this.bungeecord = Configs.SPIGOT.getConfigurationRepresentation().getYamlConfiguration().getBoolean("settings.bungeecord", false);
            DebugSender.INSTANCE.sendDebug("Bungeecord " + (this.bungeecord ? "detected" : "not detected"), true, false);

            // ------------------------------------------------------------------------------------------------------ //
            //                                                Metrics                                                 //
            // ------------------------------------------------------------------------------------------------------ //

            DebugSender.INSTANCE.sendDebug("Starting metrics. This plugin uses bStats metrics: https://bstats.org/plugin/bukkit/AntiCheatAddition/14608", true, false);
            val metrics = new Metrics(this, BSTATS_PLUGIN_ID);

            // ------------------------------------------------------------------------------------------------------ //
            //                                              Plugin hooks                                              //
            // ------------------------------------------------------------------------------------------------------ //

            // Call is correct here as Bukkit always has a player api.
            if (checkForPlugin("ViaVersion", metrics)) viaAPI = Via.getAPI();
            if (checkForPlugin("floodgate", metrics)) floodgateApi = FloodgateApi.getInstance();

            // ------------------------------------------------------------------------------------------------------ //
            //                                                Features                                                //
            // ------------------------------------------------------------------------------------------------------ //

            // Managers
            this.registerListener(new User.UserListener());

            // Load the module manager
            //noinspection ResultOfMethodCallIgnored
            ModuleManager.getModuleMap();

            // Data storage
            DataUpdaterEvents.INSTANCE.register();

            // Commands
            this.getCommand(MainCommand.INSTANCE.getName()).setExecutor(MainCommand.INSTANCE);

            // ------------------------------------------------------------------------------------------------------ //
            //                                           Enabled-Debug + API                                          //
            // ------------------------------------------------------------------------------------------------------ //
            this.getLogger().info(this.getName() + " Version " + this.getDescription().getVersion() + " enabled");
            DebugSender.INSTANCE.sendDebug("AntiCheatAddition initialization completed.");
        } catch (final Exception e) {
            // ------------------------------------------------------------------------------------------------------ //
            //                                              Failed loading                                            //
            // ------------------------------------------------------------------------------------------------------ //
            this.getLogger().log(Level.SEVERE, "Loading failed:\n", e);
        }
    }

    @Override
    public void onDisable()
    {
        // Plugin is already disabled -> DebugSender is not allowed to register a task
        DebugSender.INSTANCE.setAllowedToRegisterTasks(false);

        // Remove all the Listeners, PacketListeners
        ProtocolLibrary.getProtocolManager().removePacketListeners(this);
        HandlerList.unregisterAll(AntiCheatAddition.getInstance());

        DataUpdaterEvents.INSTANCE.unregister();

        DebugSender.INSTANCE.sendDebug("AntiCheatAddition disabled.", true, false);
        DebugSender.INSTANCE.sendDebug(" ", true, false);
    }
}
