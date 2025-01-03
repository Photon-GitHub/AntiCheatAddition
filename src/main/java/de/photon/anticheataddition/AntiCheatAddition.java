package de.photon.anticheataddition;

import com.github.retrooper.packetevents.PacketEvents;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.ViaAPI;
import de.photon.anticheataddition.commands.MainCommand;
import de.photon.anticheataddition.modules.ModuleManager;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.DataUpdaterEvents;
import de.photon.anticheataddition.user.data.subdata.BrandChannelData;
import de.photon.anticheataddition.util.config.Configs;
import de.photon.anticheataddition.util.log.Log;
import de.photon.anticheataddition.util.pluginmessage.MessageChannel;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.Nullable;

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

    public static final String ANTICHEAT_ADDITION_PREFIX = ChatColor.AQUA + "[AntiCheatAddition] " + ChatColor.GRAY;

    @Setter(AccessLevel.PROTECTED)
    @Getter
    private static AntiCheatAddition instance;

    @Getter(lazy = true)
    private final FileConfiguration config = generateConfig();
    @Nullable
    private ViaAPI<?> viaAPI;
    @Nullable
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
        getInstance().getLogger().info(pluginName + (enabled ? " hooked" : " not found"));
        metrics.addCustomChart(new SimplePie(pluginName.toLowerCase(Locale.ROOT), enabled ? () -> "Yes" : () -> "No"));
        return enabled;
    }

    @Override
    public void onLoad()
    {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable()
    {
        try {
            // Now needs to be done via this ugly way as the original way caused a loading error.
            setInstance(this);

            // Setup logging.
            Log.INSTANCE.bukkitSetup();

            // ------------------------------------------------------------------------------------------------------ //
            //                                      Unsupported server version                                        //
            // ------------------------------------------------------------------------------------------------------ //
            Log.info(() -> "Server version " + ServerVersion.ACTIVE.getVersionOutputString() + " detected.");

            if (!ServerVersion.ACTIVE.isSupported()) {
                getLogger().severe("Server version is not supported.");
                getLogger().severe(() -> "Supported versions: " + ServerVersion.ALL_SUPPORTED_VERSIONS.stream().map(ServerVersion::getVersionOutputString).collect(Collectors.joining(", ")));
                return;
            }

            // ------------------------------------------------------------------------------------------------------ //
            //                                               Bungeecord                                               //
            // ------------------------------------------------------------------------------------------------------ //

            this.bungeecord = Configs.SPIGOT.getConfigurationRepresentation().getYamlConfiguration().getBoolean("settings.bungeecord", false);
            Log.info(() -> "Bungeecord " + (this.bungeecord ? "detected" : "not detected"));

            // ------------------------------------------------------------------------------------------------------ //
            //                                                Metrics                                                 //
            // ------------------------------------------------------------------------------------------------------ //

            Log.info(() -> "Starting metrics. This plugin uses bStats metrics: https://bstats.org/plugin/bukkit/AntiCheatAddition/14608");
            final var metrics = new Metrics(this, BSTATS_PLUGIN_ID);

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
            MessageChannel.MC_BRAND_CHANNEL.registerIncomingChannel(new BrandChannelData.BrandChannelMessageListener());
            this.registerListener(DataUpdaterEvents.INSTANCE);

            // Commands
            final var mainCommand = new MainCommand(this.getDescription().getVersion());
            final var commandToRegister = this.getCommand(mainCommand.getName());
            if (commandToRegister == null) {
                Log.severe(() -> "Could not register command " + mainCommand.getName());
                return;
            }
            commandToRegister.setExecutor(mainCommand);
            commandToRegister.setTabCompleter(mainCommand);

            // PacketEvents
            PacketEvents.getAPI().init();

            // ------------------------------------------------------------------------------------------------------ //
            //                                           Enabled-Debug + API                                          //
            // ------------------------------------------------------------------------------------------------------ //
            Log.info(() -> this.getName() + " Version " + this.getDescription().getVersion() + " enabled");
            Log.fine(() -> "AntiCheatAddition initialization completed.");
            Log.finest(() -> "Full debug is active.");
        } catch (final Exception e) {
            // ------------------------------------------------------------------------------------------------------ //
            //                                              Failed loading                                            //
            // ------------------------------------------------------------------------------------------------------ //
            getLogger().log(Level.SEVERE, "Loading failed:\n", e);
        }
    }

    @Override
    public void onDisable()
    {
        // Remove all the Listeners, PacketListeners
        PacketEvents.getAPI().terminate();
        HandlerList.unregisterAll(this);

        // Unregister all plugin channels
        Bukkit.getMessenger().unregisterIncomingPluginChannel(this);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(this);

        getLogger().info("AntiCheatAddition disabled.");
        getLogger().info(" ");

        // Close the log handlers (file locking, etc.)
        Log.INSTANCE.close();
    }
}
