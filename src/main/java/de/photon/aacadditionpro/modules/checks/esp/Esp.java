package de.photon.aacadditionpro.modules.checks.esp;

import com.google.common.collect.ImmutableMap;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ListenerModule;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.util.files.configs.Configs;
import de.photon.aacadditionpro.util.messaging.VerboseSender;
import de.photon.aacadditionpro.util.visibility.HideMode;
import de.photon.aacadditionpro.util.visibility.PlayerInformationModifier;
import de.photon.aacadditionpro.util.visibility.informationmodifiers.InformationObfuscator;
import de.photon.aacadditionpro.util.visibility.informationmodifiers.PlayerHider;
import de.photon.aacadditionpro.util.world.LocationUtils;
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
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class Esp implements ListenerModule
{
    @Getter
    private static final Esp instance = new Esp();
    final ExecutorService pairExecutor = Executors.newCachedThreadPool();
    final AtomicLong activeCalculations = new AtomicLong(0);
    private final PlayerInformationModifier fullHider = new PlayerHider();
    private final PlayerInformationModifier informationOnlyHider = new InformationObfuscator();
    // The auto-config-data
    boolean hideAfterRenderDistance = true;
    int defaultTrackingRange;
    Map<UUID, Integer> playerTrackingRanges;

    private Thread supplierThread;

    @Override
    public void enable()
    {
        fullHider.registerListeners();
        informationOnlyHider.registerListeners();

        // ---------------------------------------------------- Auto-configuration ----------------------------------------------------- //
        final long updateTicksConfig = AACAdditionPro.getInstance().getConfig().getLong(this.getConfigString() + ".update_ticks");
        final AtomicLong updateTicks = new AtomicLong(updateTicksConfig);
        final ConfigurationSection worlds = Configs.SPIGOT.getConfigurationRepresentation().getYamlConfiguration().getConfigurationSection("world-settings");
        final ImmutableMap.Builder<UUID, Integer> rangeBuilder = ImmutableMap.builder();

        int currentPlayerTrackingRange;
        for (final String worldName : worlds.getKeys(false)) {
            currentPlayerTrackingRange = worlds.getInt(worldName + ".entity-tracking-range.players");

            // Square
            currentPlayerTrackingRange *= currentPlayerTrackingRange;

            // Do the maths inside here as reading from a file takes longer than calculating this.
            // 19321 == 139^2 as of the maximum range of the block-iterator
            if (currentPlayerTrackingRange > 19321) {
                hideAfterRenderDistance = false;
                currentPlayerTrackingRange = 19321;
            }

            if ("default".equals(worldName)) {
                defaultTrackingRange = currentPlayerTrackingRange;
            } else {
                final World correspondingWorld = Bukkit.getWorld(worldName);

                if (correspondingWorld != null) {
                    rangeBuilder.put(correspondingWorld.getUID(), currentPlayerTrackingRange);
                }
            }
        }

        this.playerTrackingRanges = rangeBuilder.build();

        // ----------------------------------------------------------- Task ------------------------------------------------------------ //

        // Use ArrayDeque as we can
        final Deque<User> users = new ArrayDeque<>(1500);

        class EspSupplierThread extends Thread
        {
            @SuppressWarnings({"InfiniteLoopStatement", "BusyWait"})
            @Override
            public void run()
            {
                try {
                    while (true) {
                        if (activeCalculations.get() > 0) {
                            VerboseSender.getInstance().sendVerboseMessage("Did not finish ESP cycle. Consider upgrading your hardware or increasing update_ticks if this message shows regularly.", false, true);
                            // Increase updateTicks to not further lag the server.
                            updateTicks.addAndGet(3);
                        } else {
                            // Decrease updateTicks slowly if possible.
                            if (updateTicks.get() > updateTicksConfig) {
                                updateTicks.getAndDecrement();
                            }
                        }

                        // Put all users in a Queue for fast removal.
                        // Ignore spectators.
                        for (User user : UserManager.getUsersUnwrapped()) {
                            if (user.getPlayer().getGameMode() != GameMode.SPECTATOR) {
                                users.add(user);
                            }
                        }

                        // Iterate through all player-constellations
                        while (!users.isEmpty()) {
                            // Remove the finished player to reduce the amount of added entries.
                            // This makes sure the player won't have a connection with himself.
                            // Remove the last object for better array performance.
                            final User observingUser = users.removeLast();

                            // All users can potentially be seen
                            for (final User watched : users) {
                                // The players are in the same world
                                if (LocationUtils.inSameWorld(observingUser.getPlayer(), watched.getPlayer())) {
                                    pairExecutor.submit(new EspPairRunnable(observingUser, watched));
                                    activeCalculations.incrementAndGet();
                                }
                            }
                        }
                        Thread.sleep(50 * updateTicks.get());
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
                updatePairHideMode(spectator, user, HideMode.NONE);
            }
        }
    }

    /**
     * Changes the hide mode for both specified {@link User}s.
     */
    void updatePairHideMode(final User first, final User second, final HideMode hideMode)
    {
        updateHideMode(first, second.getPlayer(), hideMode);
        updateHideMode(second, first.getPlayer(), hideMode);
    }

    // No need to synchronize hiddenPlayers as it is accessed in a synchronized task.
    void updateHideMode(final User observer, final Player watched, final HideMode hideMode)
    {
        final Player observingPlayer = observer.getPlayer();

        // Observer might have left by now.
        if (observingPlayer != null && watched != null) {
            // There is no need to manually check if something has changed as the PlayerInformationModifiers already
            // do that.
            switch (hideMode) {
                case FULL:
                    // FULL: fullHider active, informationOnlyHider inactive
                    this.informationOnlyHider.unModifyInformation(observingPlayer, watched);
                    this.fullHider.modifyInformation(observingPlayer, watched);
                    break;
                case INFORMATION_ONLY:
                    // INFORMATION_ONLY: fullHider inactive, informationOnlyHider active
                    this.fullHider.unModifyInformation(observingPlayer, watched);
                    this.informationOnlyHider.modifyInformation(observingPlayer, watched);
                    break;
                case NONE:
                    // NONE: fullHider inactive, informationOnlyHider inactive
                    this.informationOnlyHider.unModifyInformation(observingPlayer, watched);
                    this.fullHider.unModifyInformation(observingPlayer, watched);
                    break;
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