package de.photon.AACAdditionPro.modules.checks;

import com.google.common.collect.ImmutableMap;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ServerVersion;
import de.photon.AACAdditionPro.modules.ListenerModule;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.files.configs.Configs;
import de.photon.AACAdditionPro.util.mathematics.Hitbox;
import de.photon.AACAdditionPro.util.mathematics.VectorUtils;
import de.photon.AACAdditionPro.util.visibility.HideMode;
import de.photon.AACAdditionPro.util.visibility.PlayerInformationModifier;
import de.photon.AACAdditionPro.util.visibility.informationmodifiers.InformationObfuscator;
import de.photon.AACAdditionPro.util.visibility.informationmodifiers.PlayerHider;
import de.photon.AACAdditionPro.util.world.BlockUtils;
import de.photon.AACAdditionPro.util.world.ChunkUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Esp implements ListenerModule
{
    // The auto-config-data
    private static int DEFAULT_TRACKING_RANGE;
    private Map<UUID, Integer> playerTrackingRanges;
    private boolean hideAfterRenderDistance = true;

    private int updateMillis;

    // The real MAX_FOV is 110 (quake pro), which results in 150° according to tests.
    // 150° + 15° (compensation) = 165°
    private static final double MAX_FOV = Math.toRadians(165D);

    // Use a LinkedList design for optimal storage usage as the amount of bypassed / spectator players cannot be estimated.
    private final Queue<User> users = new ArrayDeque<>();

    private final PlayerInformationModifier fullHider = new PlayerHider();
    private final PlayerInformationModifier informationOnlyHider = new InformationObfuscator();

    // The task number for Bukkit's internal systems
    private int taskNumber;

    @Override
    public void enable()
    {
        // ---------------------------------------------------- Auto-configuration ----------------------------------------------------- //
        final int updateTicks = AACAdditionPro.getInstance().getConfig().getInt(this.getConfigString() + ".update_ticks");
        updateMillis = 50 * updateTicks;

        final ConfigurationSection worlds = Configs.SPIGOT.getConfigurationRepresentation().getYamlConfiguration().getConfigurationSection("world-settings");

        final ImmutableMap.Builder<UUID, Integer> rangeBuilder = ImmutableMap.builder();

        int currentPlayerTrackingRange;
        for (final String world : worlds.getKeys(false)) {
            currentPlayerTrackingRange = worlds.getInt(world + ".entity-tracking-range.players");

            // Square
            currentPlayerTrackingRange *= currentPlayerTrackingRange;

            // Do the maths inside here as reading from a file takes longer than calculating this.
            // 19321 == 139^2 as of the maximum range of the block-iterator
            if (currentPlayerTrackingRange > 19321) {
                hideAfterRenderDistance = false;
                currentPlayerTrackingRange = 19321;
            }

            if (world.equals("default")) {
                DEFAULT_TRACKING_RANGE = currentPlayerTrackingRange;
            }
            else {
                final World correspondingWorld = Bukkit.getWorld(world);

                if (correspondingWorld != null) {
                    rangeBuilder.put(correspondingWorld.getUID(), currentPlayerTrackingRange);
                }
            }
        }

        this.playerTrackingRanges = rangeBuilder.build();

        // ----------------------------------------------------------- Task ------------------------------------------------------------ //

        taskNumber = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                AACAdditionPro.getInstance(),
                () -> {
                    // Put all users in a Queue for fast removal.
                    users.addAll(UserManager.getUsersUnwrapped());

                    final ExecutorService pairExecutor = Executors.newWorkStealingPool();

                    // Iterate through all player-constellations
                    while (!users.isEmpty()) {
                        // Remove the finished player to reduce the amount of added entries.
                        // This makes sure the player won't have a connection with himself.
                        final User observingUser = users.remove();

                        // Do not process spectators.
                        if (observingUser.getPlayer().getGameMode() == GameMode.SPECTATOR) {
                            continue;
                        }

                        // All users can potentially be seen
                        for (final User watched : users) {
                            // The watched player is also not in Spectator mode
                            if (watched.getPlayer().getGameMode() != GameMode.SPECTATOR &&
                                // The players are in the same world
                                observingUser.getPlayer().getWorld().getUID().equals(watched.getPlayer().getWorld().getUID()))
                            {
                                // The users are always in the same world (see above)
                                final double pairDistanceSquared = observingUser.getPlayer().getLocation().distanceSquared(watched.getPlayer().getLocation());

                                pairExecutor.execute(() -> {
                                    // Less than 1 block distance
                                    // Everything (smaller than 1)^2 will result in something smaller than 1
                                    if (pairDistanceSquared < 1) {
                                        updatePairHideMode(observingUser, watched, HideMode.NONE);
                                        return;
                                    }

                                    if (pairDistanceSquared > this.playerTrackingRanges.getOrDefault(observingUser.getPlayer().getWorld().getUID(), DEFAULT_TRACKING_RANGE)) {
                                        updatePairHideMode(observingUser, watched, hideAfterRenderDistance ?
                                                                                   HideMode.FULL :
                                                                                   HideMode.NONE);
                                        return;
                                    }

                                    // Update hide mode in both directions.
                                    updateHideMode(observingUser, watched.getPlayer(),
                                                   canSee(observingUser, watched) ?
                                                   // Is the user visible
                                                   HideMode.NONE :
                                                   // If the observed player is sneaking hide him fully
                                                   (watched.getPlayer().isSneaking() ?
                                                    HideMode.FULL :
                                                    HideMode.INFORMATION_ONLY));

                                    updateHideMode(watched, observingUser.getPlayer(),
                                                   canSee(watched, observingUser) ?
                                                   // Is the user visible
                                                   HideMode.NONE :
                                                   // If the observed player is sneaking hide him fully
                                                   (observingUser.getPlayer().isSneaking() ?
                                                    HideMode.FULL :
                                                    HideMode.INFORMATION_ONLY));
                                });
                            }
                        }
                    }

                    pairExecutor.shutdown();

                    try {
                        if (!pairExecutor.awaitTermination(updateMillis, TimeUnit.MILLISECONDS)) {
                            VerboseSender.getInstance().sendVerboseMessage("Could not finish ESP cycle. Please consider upgrading your hardware or increasing the update_ticks option in the config if this message appears in large quantities.", false, true);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // Update_Ticks: the refresh-rate of the check.
                }, 0L, updateTicks);
    }

    /**
     * Determines if two {@link User}s can see each other.
     */
    private boolean canSee(final User observerUser, final User watchedUser)
    {
        final Player observer = observerUser.getPlayer();
        final Player watched = watchedUser.getPlayer();

        // Not bypassed
        if (observerUser.isBypassed(this.getModuleType()) ||
            // Has not logged in recently to prevent bugs
            observerUser.getLoginData().recentlyUpdated(0, 3000) ||
            // Glowing handling
            // Glowing does not exist in 1.8.8
            (ServerVersion.getActiveServerVersion() != ServerVersion.MC188 &&
             // If an entity is glowing it can always be seen.
             watched.hasPotionEffect(PotionEffectType.GLOWING)))
        {
            return true;
        }

        // ----------------------------------- Calculation ---------------------------------- //

        //canSee = observer.hasLineOfSight(watched);
        final Vector[] cameraVectors = VectorUtils.getCameraVectors(observer);

        // Get the Vectors of the hitbox to check.
        final Vector[] watchedHitboxVectors = (watched.isSneaking() ?
                                               Hitbox.ESP_SNEAKING_PLAYER :
                                               Hitbox.ESP_PLAYER).getCalculationVectors(watched.getLocation(), true);

        // The distance of the intersections in the same block is equal as of the
        // BlockIterator mechanics.
        final Set<Double> lastIntersectionsCache = new HashSet<>();

        for (Vector cameraVector : cameraVectors) {
            for (final Vector destinationVector : watchedHitboxVectors) {
                final Location start = cameraVector.toLocation(observer.getWorld());
                // The resulting Vector
                // The camera is not blocked by non-solid blocks
                // Vector is intersecting with some blocks
                //
                // Cloning IS needed as we are in a second loop.
                final Vector between = destinationVector.clone().subtract(cameraVector);

                // ---------------------------------------------- FOV ----------------------------------------------- //
                final Vector cameraRotation = cameraVector.clone().subtract(observer.getLocation().toVector());

                if (cameraRotation.angle(between) > MAX_FOV) {
                    continue;
                }

                // ---------------------------------------- Cache Calculation --------------------------------------- //

                // Make sure the chunks are loaded.
                if (!ChunkUtils.areChunksLoadedBetweenLocations(start, start.clone().add(between))) {
                    // If the chunks are not loaded assume the players can see each other.
                    return true;
                }

                boolean cacheHit = false;

                Location cacheLocation;
                for (Double length : lastIntersectionsCache) {
                    cacheLocation = start.clone().add(between.clone().normalize().multiply(length));

                    // Not yet cached.
                    if (length == 0) {
                        continue;
                    }

                    final Material type = cacheLocation.getBlock().getType();

                    if (BlockUtils.isReallyOccluding(type) && type.isSolid()) {
                        cacheHit = true;
                        break;
                    }
                }

                if (cacheHit) {
                    continue;
                }

                // --------------------------------------- Normal Calculation --------------------------------------- //

                final double intersect = VectorUtils.getDistanceToFirstIntersectionWithBlock(start, between);

                // No intersection found
                if (intersect == 0) {
                    return true;
                }

                lastIntersectionsCache.add(intersect);
            }
        }

        // Low probability to help after the camera view was changed. -> clearing
        lastIntersectionsCache.clear();
        return false;
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

    private void updatePairHideMode(final User first, final User second, final HideMode hideMode)
    {
        updateHideMode(first, second.getPlayer(), hideMode);
        updateHideMode(second, first.getPlayer(), hideMode);
    }

    // No need to synchronize hiddenPlayers as it is accessed in a synchronized task.
    private void updateHideMode(final User observer, final Player watched, final HideMode hideMode)
    {
        // unModifyInformation and modifyInformation are not thread-safe.
        Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> {
            // Observer might have left by now.
            if (observer != null) {
                // There is no need to manually check if something has changed as the PlayerInformationModifiers already
                // do that.
                switch (hideMode) {
                    case FULL:
                        // FULL: fullHider active, informationOnlyHider inactive
                        informationOnlyHider.unModifyInformation(observer.getPlayer(), watched);
                        fullHider.modifyInformation(observer.getPlayer(), watched);
                        break;
                    case INFORMATION_ONLY:
                        // INFORMATION_ONLY: fullHider inactive, informationOnlyHider active
                        fullHider.unModifyInformation(observer.getPlayer(), watched);
                        informationOnlyHider.modifyInformation(observer.getPlayer(), watched);
                        break;
                    case NONE:
                        // NONE: fullHider inactive, informationOnlyHider inactive
                        informationOnlyHider.unModifyInformation(observer.getPlayer(), watched);
                        fullHider.unModifyInformation(observer.getPlayer(), watched);
                        break;
                }
            }
        });
    }

    @Override
    public void disable()
    {
        Bukkit.getScheduler().cancelTask(taskNumber);
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.ESP;
    }
}