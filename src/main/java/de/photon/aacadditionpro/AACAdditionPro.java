package de.photon.aacadditionpro;

import com.comphenix.protocol.ProtocolLibrary;
import com.google.common.base.Preconditions;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.ViaAPI;
import de.photon.aacadditionpro.commands.MainCommand;
import de.photon.aacadditionpro.modules.ModuleManager;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.DataUpdaterEvents;
import de.photon.aacadditionpro.util.config.Configs;
import de.photon.aacadditionpro.util.oldmessaging.DebugSender;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import me.konsolas.aac.api.AACAPI;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Getter
public class AACAdditionPro extends JavaPlugin
{
    /**
     * The expected players to be on the entire server across all worlds.
     */
    public static final int SERVER_EXPECTED_PLAYERS = 150;

    /**
     * The expected player to be in a world.
     */
    public static final int WORLD_EXPECTED_PLAYERS = 35;

    private static final int BSTATS_PLUGIN_ID = 3265;

    @Setter(AccessLevel.PROTECTED)
    @Getter private static AACAdditionPro instance;

    @Getter(lazy = true) private final FileConfiguration config = generateConfig();
    private ViaAPI<?> viaAPI;
    private AACAPI aacapi = null;

    private boolean bungeecord = false;

    /**
     * Registers a new {@link Listener} for AACAdditionPro.
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

    @Override
    public void onEnable()
    {
        try {
            // Now needs to be done via this ugly way as the original way did lead to a loading error.
            setInstance(this);

            // ------------------------------------------------------------------------------------------------------ //
            //                                      Unsupported server version                                        //
            // ------------------------------------------------------------------------------------------------------ //
            if (!ServerVersion.getActiveServerVersion().isSupported()) {
                DebugSender.getInstance().sendDebug("Server version is not supported.", true, true);
                DebugSender.getInstance().sendDebug("Supported versions: " + Arrays.stream(ServerVersion.values()).filter(ServerVersion::isSupported).map(ServerVersion::getVersionOutputString).collect(Collectors.joining(", ")),
                                                    true, true);
                return;
            }

            // ------------------------------------------------------------------------------------------------------ //
            //                                               Bungeecord                                               //
            // ------------------------------------------------------------------------------------------------------ //

            this.bungeecord = Configs.SPIGOT.getConfigurationRepresentation().getYamlConfiguration().getBoolean("settings.bungeecord", false);
            DebugSender.getInstance().sendDebug("Bungeecord " + (this.bungeecord ? "detected" : "not detected"), true, false);

            // ------------------------------------------------------------------------------------------------------ //
            //                                                Metrics                                                 //
            // ------------------------------------------------------------------------------------------------------ //

            DebugSender.getInstance().sendDebug("Starting metrics. This plugin uses bStats metrics: https://bstats.org/plugin/bukkit/AACAdditionPro/3265", true, false);
            val metrics = new Metrics(this, BSTATS_PLUGIN_ID);

            // The first getConfig call will automatically saveToFile and cache the config.

            // ------------------------------------------------------------------------------------------------------ //
            //                                              Plugin hooks                                              //
            // ------------------------------------------------------------------------------------------------------ //

            // Call is correct here as Bukkit always has a player api.
            val viaEnabled = this.getServer().getPluginManager().isPluginEnabled("ViaVersion");
            DebugSender.getInstance().sendDebug("ViaVersion " + (viaEnabled ? "hooked" : "not found"), true, false);
            if (viaEnabled) viaAPI = Via.getAPI();

            metrics.addCustomChart(new SimplePie("viaversion", () -> viaEnabled ? "Used" : "Not used"));

            // ------------------------------------------------------------------------------------------------------ //
            //                                                Features                                                //
            // ------------------------------------------------------------------------------------------------------ //

            // Managers
            this.registerListener(new User.UserListener());
            // Load the module manager
            //noinspection ResultOfMethodCallIgnored
            ModuleManager.getModuleMap();

            // ------------------------------------------------------------------------------------------------------ //
            //                                                AAC hook                                                //
            // ------------------------------------------------------------------------------------------------------ //

            // Call is correct here as Bukkit always has a player api.
            if (this.getServer().getPluginManager().isPluginEnabled("AAC5")) {
                if (this.getConfig().getBoolean("UseAACFeatureSystem", true)) {
                    this.aacapi = Preconditions.checkNotNull(Bukkit.getServicesManager().load(AACAPI.class), "Did not find AAC API while hooking.");
                    this.aacapi.registerCustomFeatureProvider(ModuleManager.getCustomFeatureProvider());
                    DebugSender.getInstance().sendDebug("AAC hooked", true, false);
                    metrics.addCustomChart(new SimplePie("aac", () -> "Hooked"));
                } else {
                    metrics.addCustomChart(new SimplePie("aac", () -> "Used"));
                    DebugSender.getInstance().sendDebug("AAC found, but not hooked", true, false);
                }
            } else {
                metrics.addCustomChart(new SimplePie("aac", () -> "Not used"));
                DebugSender.getInstance().sendDebug("AAC not found", true, false);
            }

            // Data storage
            DataUpdaterEvents.INSTANCE.register();

            // Commands
            this.getCommand(MainCommand.getInstance().getName()).setExecutor(MainCommand.getInstance());

            // ------------------------------------------------------------------------------------------------------ //
            //                                           Enabled-Debug + API                                          //
            // ------------------------------------------------------------------------------------------------------ //
            this.getLogger().info(this.getName() + " Version " + this.getDescription().getVersion() + " enabled");
            DebugSender.getInstance().sendDebug("AACAdditionPro initialization completed.");
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
        DebugSender.getInstance().setAllowedToRegisterTasks(false);

        // Remove all the Listeners, PacketListeners
        ProtocolLibrary.getProtocolManager().removePacketListeners(this);
        HandlerList.unregisterAll(AACAdditionPro.getInstance());

        DataUpdaterEvents.INSTANCE.unregister();

        DebugSender.getInstance().sendDebug("AACAdditionPro disabled.", true, false);
        DebugSender.getInstance().sendDebug(" ", true, false);
    }
}
