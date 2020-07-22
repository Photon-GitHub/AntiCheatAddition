package de.photon.aacadditionpro;

import com.comphenix.protocol.ProtocolLibrary;
import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.command.MainCommand;
import de.photon.aacadditionpro.events.APILoadedEvent;
import de.photon.aacadditionpro.modules.ModuleManager;
import de.photon.aacadditionpro.modules.additions.BrandHider;
import de.photon.aacadditionpro.modules.additions.GuiInject;
import de.photon.aacadditionpro.modules.additions.LogBot;
import de.photon.aacadditionpro.modules.checks.AutoEat;
import de.photon.aacadditionpro.modules.checks.AutoPotion;
import de.photon.aacadditionpro.modules.checks.Fastswitch;
import de.photon.aacadditionpro.modules.checks.ImpossibleChat;
import de.photon.aacadditionpro.modules.checks.SkinBlinker;
import de.photon.aacadditionpro.modules.checks.Teaming;
import de.photon.aacadditionpro.modules.checks.autofish.AutoFish;
import de.photon.aacadditionpro.modules.checks.esp.Esp;
import de.photon.aacadditionpro.modules.checks.inventory.Inventory;
import de.photon.aacadditionpro.modules.checks.keepalive.KeepAlive;
import de.photon.aacadditionpro.modules.checks.packetanalysis.PacketAnalysis;
import de.photon.aacadditionpro.modules.checks.scaffold.Scaffold;
import de.photon.aacadditionpro.modules.checks.tower.Tower;
import de.photon.aacadditionpro.modules.clientcontrol.BetterSprintingControl;
import de.photon.aacadditionpro.modules.clientcontrol.DamageIndicator;
import de.photon.aacadditionpro.modules.clientcontrol.FiveZigControl;
import de.photon.aacadditionpro.modules.clientcontrol.ForgeControl;
import de.photon.aacadditionpro.modules.clientcontrol.LabyModControl;
import de.photon.aacadditionpro.modules.clientcontrol.LiteLoaderControl;
import de.photon.aacadditionpro.modules.clientcontrol.OldLabyModControl;
import de.photon.aacadditionpro.modules.clientcontrol.PXModControl;
import de.photon.aacadditionpro.modules.clientcontrol.SchematicaControl;
import de.photon.aacadditionpro.modules.clientcontrol.VapeControl;
import de.photon.aacadditionpro.modules.clientcontrol.VersionControl;
import de.photon.aacadditionpro.modules.clientcontrol.WorldDownloaderControl;
import de.photon.aacadditionpro.user.DataUpdaterEvents;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.util.VerboseSender;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.ViaAPI;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class AACAdditionPro extends JavaPlugin
{
    private static AACAdditionPro instance;

    /**
     * Indicates if the loading process is completed.
     */
    @Getter
    private boolean loaded = false;
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
        return instance;
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

    @NotNull
    @Override
    public FileConfiguration getConfig()
    {
        if (cachedConfig == null) {
            this.saveDefaultConfig();
            cachedConfig = YamlConfiguration.loadConfiguration(new File(this.getDataFolder(), "config.yml"));
        }

        return cachedConfig;
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
                VerboseSender.getInstance().sendVerboseMessage("Server version is not supported.", true, true);

                // Print the complete message
                VerboseSender.getInstance().sendVerboseMessage(
                        "Supported versions: " + Arrays.stream(ServerVersion.values()).filter(ServerVersion::isSupported).map(ServerVersion::getVersionOutputString).collect(Collectors.joining(", ")),
                        true, true);
                return;
            }

            // ------------------------------------------------------------------------------------------------------ //
            //                                                Metrics                                                 //
            // ------------------------------------------------------------------------------------------------------ //

            final Metrics metrics = new Metrics(this);

            // The first getConfig call will automatically saveToFile and cache the config.

            // ------------------------------------------------------------------------------------------------------ //
            //                                              Plugin hooks                                              //
            // ------------------------------------------------------------------------------------------------------ //

            // Call is correct here as Bukkit always has a player api.
            if (this.getServer().getPluginManager().getPlugin("ViaVersion") != null) {
                //noinspection unchecked
                viaAPI = Via.getAPI();
                metrics.addCustomChart(new Metrics.SimplePie("viaversion", () -> "Used"));
            } else {
                metrics.addCustomChart(new Metrics.SimplePie("viaversion", () -> "Not used"));
            }


            // ------------------------------------------------------------------------------------------------------ //
            //                                                Features                                                //
            // ------------------------------------------------------------------------------------------------------ //

            // Managers
            this.registerListener(new UserManager());
            this.moduleManager = new ModuleManager(ImmutableSet.of(
                    // Additions
                    new BrandHider(),
                    new GuiInject(),
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
                    new AutoEat(),
                    new AutoFish(),
                    new AutoPotion(),
                    Esp.getInstance(),
                    new Fastswitch(),
                    new ImpossibleChat(),
                    new Inventory(),
                    new KeepAlive(),
                    new PacketAnalysis(),
                    new Scaffold(),
                    new SkinBlinker(),
                    new Teaming(),
                    Tower.getInstance())
            );

            // Data storage
            DataUpdaterEvents.INSTANCE.register();

            // Commands
            this.getCommand(MainCommand.getInstance().getMainCommandName()).setExecutor(MainCommand.getInstance());

            // ------------------------------------------------------------------------------------------------------ //
            //                                          Enabled-Verbose + API                                         //
            // ------------------------------------------------------------------------------------------------------ //
            this.getLogger().info(this.getName() + " Version " + this.getDescription().getVersion() + " enabled");

            // API loading finished
            this.loaded = true;
            this.getServer().getPluginManager().callEvent(new APILoadedEvent());

            VerboseSender.getInstance().sendVerboseMessage("AACAdditionPro initialization completed.");
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
        VerboseSender.getInstance().setAllowedToRegisterTasks(false);

        // Remove all the Listeners, PacketListeners
        ProtocolLibrary.getProtocolManager().removePacketListeners(this);
        HandlerList.unregisterAll(AACAdditionPro.getInstance());

        DataUpdaterEvents.INSTANCE.unregister();

        VerboseSender.getInstance().sendVerboseMessage("AACAdditionPro disabled.", true, false);
        VerboseSender.getInstance().sendVerboseMessage(" ", true, false);

        // Task scheduling
        loaded = false;
    }
}
