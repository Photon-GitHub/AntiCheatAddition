package de.photon.aacadditionpro;

import com.comphenix.protocol.ProtocolLibrary;
import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.commands.MainCommand;
import de.photon.aacadditionpro.modules.ModuleManager;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.DataUpdaterEvents;
import de.photon.aacadditionpro.util.config.Configs;
import de.photon.aacadditionpro.util.messaging.DebugSender;
import lombok.Getter;
import lombok.val;
import me.konsolas.aac.api.AACAPI;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.ViaAPI;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Getter
public class AACAdditionPro extends JavaPlugin
{
    private static final int BSTATS_PLUGIN_ID = 3265;

    @Getter private static AACAdditionPro instance;

    @Getter(lazy = true) private final FileConfiguration config = generateConfig();
    private ViaAPI<Player> viaAPI;
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
            instance = this;

            // ------------------------------------------------------------------------------------------------------ //
            //                                      Unsupported server version                                        //
            // ------------------------------------------------------------------------------------------------------ //
            if (ServerVersion.getActiveServerVersion() == null ||
                // Unsupported
                !ServerVersion.getActiveServerVersion().isSupported())
            {
                DebugSender.getInstance().sendVerboseMessage("Server version is not supported.", true, true);

                // Print the complete message
                DebugSender.getInstance().sendVerboseMessage(
                        "Supported versions: " + Arrays.stream(ServerVersion.values()).filter(ServerVersion::isSupported).map(ServerVersion::getVersionOutputString).collect(Collectors.joining(", ")),
                        true, true);
                return;
            }

            // ------------------------------------------------------------------------------------------------------ //
            //                                               Bungeecord                                               //
            // ------------------------------------------------------------------------------------------------------ //

            this.bungeecord = Configs.SPIGOT.getConfigurationRepresentation().getYamlConfiguration().getBoolean("settings.bungeecord", false);
            DebugSender.getInstance().sendVerboseMessage("Bungeecord " + (this.bungeecord ?
                                                                          "detected" :
                                                                          "not detected"), true, false);

            // ------------------------------------------------------------------------------------------------------ //
            //                                                Metrics                                                 //
            // ------------------------------------------------------------------------------------------------------ //

            DebugSender.getInstance().sendVerboseMessage("Starting metrics. This plugin uses bStats metrics: https://bstats.org/plugin/bukkit/AACAdditionPro/3265", true, false);
            val metrics = new Metrics(this, BSTATS_PLUGIN_ID);

            // The first getConfig call will automatically saveToFile and cache the config.

            // ------------------------------------------------------------------------------------------------------ //
            //                                              Plugin hooks                                              //
            // ------------------------------------------------------------------------------------------------------ //

            // Call is correct here as Bukkit always has a player api.
            if (this.getServer().getPluginManager().isPluginEnabled("ViaVersion")) {
                //noinspection unchecked
                viaAPI = Via.getAPI();
                metrics.addCustomChart(new Metrics.SimplePie("viaversion", () -> "Used"));
                DebugSender.getInstance().sendVerboseMessage("ViaVersion hooked", true, false);
            } else {
                metrics.addCustomChart(new Metrics.SimplePie("viaversion", () -> "Not used"));
                DebugSender.getInstance().sendVerboseMessage("ViaVersion not found", true, false);
            }


            // ------------------------------------------------------------------------------------------------------ //
            //                                                Features                                                //
            // ------------------------------------------------------------------------------------------------------ //

            // Managers
            this.registerListener(new User.UserListener());
            // Load the module manager
            //noinspection ResultOfMethodCallIgnored
            ModuleManager.getMODULES();

            // ------------------------------------------------------------------------------------------------------ //
            //                                                AAC hook                                                //
            // ------------------------------------------------------------------------------------------------------ //

            // Call is correct here as Bukkit always has a player api.
            if (this.getServer().getPluginManager().isPluginEnabled("AAC5")) {
                if (this.getConfig().getBoolean("UseAACFeatureSystem")) {
                    this.aacapi = Preconditions.checkNotNull(Bukkit.getServicesManager().load(AACAPI.class), "Did not find AAC API while hooking.");
                    this.aacapi.registerCustomFeatureProvider(this.getModuleManager().getCustomFeatureProvider());
                    DebugSender.getInstance().sendVerboseMessage("AAC hooked", true, false);
                    metrics.addCustomChart(new Metrics.SimplePie("aac", () -> "Hooked"));
                } else {
                    metrics.addCustomChart(new Metrics.SimplePie("aac", () -> "Used"));
                    DebugSender.getInstance().sendVerboseMessage("AAC found, but not hooked", true, false);
                }
            } else {
                metrics.addCustomChart(new Metrics.SimplePie("aac", () -> "Not used"));
                DebugSender.getInstance().sendVerboseMessage("AAC not found", true, false);
            }

            // Data storage
            DataUpdaterEvents.INSTANCE.register();

            // Commands
            this.getCommand(MainCommand.getInstance().getName()).setExecutor(MainCommand.getInstance());

            // ------------------------------------------------------------------------------------------------------ //
            //                                          Enabled-Verbose + API                                         //
            // ------------------------------------------------------------------------------------------------------ //
            this.getLogger().info(this.getName() + " Version " + this.getDescription().getVersion() + " enabled");
            DebugSender.getInstance().sendVerboseMessage("AACAdditionPro initialization completed.");
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
        // Plugin is already disabled -> VerboseSender is not allowed to register a task
        DebugSender.getInstance().setAllowedToRegisterTasks(false);

        // Remove all the Listeners, PacketListeners
        ProtocolLibrary.getProtocolManager().removePacketListeners(this);
        HandlerList.unregisterAll(AACAdditionPro.getInstance());

        DataUpdaterEvents.INSTANCE.unregister();

        DebugSender.getInstance().sendVerboseMessage("AACAdditionPro disabled.", true, false);
        DebugSender.getInstance().sendVerboseMessage(" ", true, false);
    }
}
