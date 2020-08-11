package de.photon.aacadditionpro.modules.checks.esp;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import de.photon.aacadditionpro.modules.ListenerModule;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.util.files.configs.Configs;
import de.photon.aacadditionpro.util.mathematics.MathUtils;
import de.photon.aacadditionpro.util.visibility.HideMode;
import de.photon.aacadditionpro.util.visibility.PlayerInformationModifier;
import de.photon.aacadditionpro.util.visibility.informationmodifiers.InformationObfuscator;
import de.photon.aacadditionpro.util.visibility.informationmodifiers.PlayerHider;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class Esp implements ListenerModule
{
    @Getter
    private static final Esp instance = new Esp();
    private static final int MAX_BLOCK_ITERATOR_RANGE_SQUARED = 139 * 139;

    // The packet hiders.
    private final PlayerInformationModifier fullHider = new PlayerHider();
    private final PlayerInformationModifier informationOnlyHider = new InformationObfuscator();
    Semaphore cycleSemaphore = new Semaphore(0);

    // The auto-config-data
    boolean hideAfterRenderDistance;
    int defaultTrackingRange;
    Map<World, Integer> playerTrackingRanges;

    // Work stealing pool as the pairs can have vastly different execution times.
    private ExecutorService pairExecutor;
    private Thread supplierThread;

    @Override
    public void enable()
    {
        // ---------------------------------------------------- Auto-configuration ----------------------------------------------------- //
        final ConfigurationSection worlds = Preconditions.checkNotNull(Configs.SPIGOT.getConfigurationRepresentation().getYamlConfiguration().getConfigurationSection("world-settings"), "World settings are not present. Aborting ESP enable.");
        final ImmutableMap.Builder<World, Integer> rangeBuilder = ImmutableMap.builder();

        int currentPlayerTrackingRange = 0;
        for (final String worldName : worlds.getKeys(false)) {
            // Squared tracking distance
            currentPlayerTrackingRange = worlds.getInt(worldName + ".entity-tracking-range.players");
            currentPlayerTrackingRange *= currentPlayerTrackingRange;

            // Check if we are over the block iterator range
            if (currentPlayerTrackingRange > MAX_BLOCK_ITERATOR_RANGE_SQUARED) {
                currentPlayerTrackingRange = MAX_BLOCK_ITERATOR_RANGE_SQUARED;
            }

            if ("default".equals(worldName)) {
                defaultTrackingRange = currentPlayerTrackingRange;
            } else {
                final World correspondingWorld = Bukkit.getWorld(worldName);

                if (correspondingWorld != null) {
                    rangeBuilder.put(correspondingWorld, currentPlayerTrackingRange);
                }
            }
        }

        this.hideAfterRenderDistance = currentPlayerTrackingRange != MAX_BLOCK_ITERATOR_RANGE_SQUARED;
        this.playerTrackingRanges = rangeBuilder.build();

        // ----------------------------------------------------------- Task ------------------------------------------------------------ //

        // Make sure the Semaphore has the correct initial value, even if the check is restarted.
        cycleSemaphore = new Semaphore(0);
        pairExecutor = Executors.newWorkStealingPool();

        // Register the packet hiders.
        fullHider.registerListeners();
        informationOnlyHider.registerListeners();

        class EspSupplierThread extends Thread
        {
            @SuppressWarnings("InfiniteLoopStatement")
            @Override
            public void run()
            {
                try {
                    final Deque<Player> players = new ArrayDeque<>();
                    Player observer;
                    int startedThreads = 0;

                    while (true) {
                        for (World world : playerTrackingRanges.keySet()) {
                            for (Player player : world.getPlayers()) {
                                if (!User.isBypassed(player, getModuleType()) && player.getGameMode() != GameMode.SPECTATOR) {
                                    players.add(player);
                                }
                            }

                            // Every iteration the number of players is reduced by 1. We start with players.size() - 1
                            // as the first player is removed right away and will not count towards the connections.
                            startedThreads += MathUtils.gaussianSumFormulaTo(players.size() - 1);

                            while (!players.isEmpty()) {
                                // Remove the finished player to reduce the amount of added entries.
                                // This makes sure the player won't have a connection with himself.
                                // Remove the last object for better array performance.
                                observer = players.removeLast();

                                for (Player watched : players) {
                                    pairExecutor.execute(new EspPairRunnable(observer, watched));
                                }
                            }
                        }

                        // Wait for all threads to finish.
                        cycleSemaphore.acquire(startedThreads);
                        startedThreads = 0;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        supplierThread = new EspSupplierThread();
        supplierThread.start();
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event)
    {
        if (event.getNewGameMode() == GameMode.SPECTATOR) {
            final User spectator = UserManager.getUser(event.getPlayer().getUniqueId());

            // Not bypassed
            if (User.isUserInvalid(spectator, this.getModuleType())) {
                return;
            }

            // Spectators can see everyone and can be seen by everyone (let vanilla handle this)
            for (User user : UserManager.getUsersUnwrapped()) {
                updatePairHideMode(spectator.getPlayer(), user.getPlayer(), HideMode.NONE);
            }
        }
    }

    /**
     * Changes the hide mode for both specified {@link User}s.
     */
    void updatePairHideMode(final Player first, final Player second, final HideMode hideMode)
    {
        updateHideMode(first, second, hideMode);
        updateHideMode(second, first, hideMode);
    }

    // No need to synchronize hiddenPlayers as it is accessed in a synchronized task.
    void updateHideMode(final Player observer, final Player watched, final HideMode hideMode)
    {
        // Observer might have left by now.
        if (observer != null && watched != null) {
            // There is no need to manually check if something has changed as the PlayerInformationModifiers already
            // do that.
            switch (hideMode) {
                case FULL:
                    // FULL: fullHider active, informationOnlyHider inactive
                    this.informationOnlyHider.unModifyInformation(observer, watched);
                    this.fullHider.modifyInformation(observer, watched);
                    break;
                case INFORMATION_ONLY:
                    // INFORMATION_ONLY: fullHider inactive, informationOnlyHider active
                    this.fullHider.unModifyInformation(observer, watched);
                    this.informationOnlyHider.modifyInformation(observer, watched);
                    break;
                case NONE:
                    // NONE: fullHider inactive, informationOnlyHider inactive
                    this.informationOnlyHider.unModifyInformation(observer, watched);
                    this.fullHider.unModifyInformation(observer, watched);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown HideMode: " + hideMode);
            }
        }
    }

    @Override
    public void disable()
    {
        supplierThread.interrupt();

        // Do not care about all the still running tasks.
        pairExecutor.shutdownNow();

        // Remove all the hiding.
        fullHider.resetTable();
        fullHider.unregisterListeners();
        informationOnlyHider.resetTable();
        informationOnlyHider.unregisterListeners();
    }

    @Override
    public boolean isSubModule()
    {
        return false;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.ESP;
    }
}