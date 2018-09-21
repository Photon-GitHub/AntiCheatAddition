package de.photon.AACAdditionPro.modules.checks;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ServerVersion;
import de.photon.AACAdditionPro.modules.Module;
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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Esp implements Module
{
    // The auto-config-data
    private double renderDistanceSquared = 0;
    private boolean hideAfterRenderDistance = true;

    private int updateMillis;

    // The real MAX_FOV is 110 (quake pro), which results in 150° according to tests.
    // 150° + 15° (compensation) = 165°
    private static final double MAX_FOV = Math.toRadians(165D);

    // Use a LinkedList design for optimal storage usage as the amount of bypassed / spectator players cannot be estimated.
    private final Deque<Pair> playerConnections = new ArrayDeque<>();

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

        for (final String world : worlds.getKeys(false))
        {
            int currentPlayerTrackingRange = worlds.getInt(world + ".entity-tracking-range.players");

            // Square
            currentPlayerTrackingRange *= currentPlayerTrackingRange;

            if (currentPlayerTrackingRange > renderDistanceSquared)
            {
                renderDistanceSquared = currentPlayerTrackingRange;

                // Do the maths inside here as reading from a file takes longer than calculating this.
                // 19321 == 139^2 as of the maximum range of the block-iterator
                if (renderDistanceSquared > 19321)
                {
                    hideAfterRenderDistance = false;
                    renderDistanceSquared = 19321;
                    break;
                }
            }
        }

        // ----------------------------------------------------------- Task ------------------------------------------------------------ //

        taskNumber = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                AACAdditionPro.getInstance(),
                () -> {
                    // Put all users in a List for fast removal.
                    final Queue<User> users = new ArrayDeque<>(UserManager.getUsersUnwrapped());

                    // Iterate through all player-constellations
                    User observingUser;
                    while (!users.isEmpty())
                    {
                        // Remove the finished player to reduce the amount of added entries.
                        // This makes sure the player won't have a connection with himself.
                        observingUser = users.remove();

                        if (observingUser.getPlayer().getGameMode() != GameMode.SPECTATOR)
                        {
                            // All users can potentially be seen
                            for (final User watched : users)
                            {
                                // The watched player is also not in Spectator mode
                                if (watched.getPlayer().getGameMode() != GameMode.SPECTATOR &&
                                    // The players are in the same world
                                    observingUser.getPlayer().getWorld().getUID().equals(watched.getPlayer().getWorld().getUID()))
                                {
                                    playerConnections.addLast(new Pair(observingUser, watched));
                                }
                            }
                        }
                    }

                    final ExecutorService pairExecutor = Executors.newWorkStealingPool();

                    Pair pair;
                    while (!playerConnections.isEmpty())
                    {
                        // Automatically empty the playerConnections
                        // Remove last entry for performance
                        pair = playerConnections.removeLast();

                        // The users are always in the same world (see abovel)
                        final double pairDistanceSquared = pair.usersOfPair[0].getPlayer().getLocation().distanceSquared(pair.usersOfPair[1].getPlayer().getLocation());

                        final Pair currentPair = pair;
                        pairExecutor.execute(() -> {
                            // Less than 1 block distance
                            // Everything (smaller than 1)^2 will result in something smaller than 1
                            if (pairDistanceSquared < 1)
                            {
                                updatePairHideMode(currentPair, HideMode.NONE);
                                return;
                            }

                            if (pairDistanceSquared > renderDistanceSquared)
                            {
                                updatePairHideMode(currentPair, hideAfterRenderDistance ?
                                                                HideMode.FULL :
                                                                HideMode.NONE);
                                return;
                            }

                            for (byte b = 0; b <= 1; b++)
                            {
                                updateHideMode(currentPair.usersOfPair[b], currentPair.usersOfPair[1 - b].getPlayer(),
                                               // Is the user visible
                                               canSee(currentPair.usersOfPair[b], currentPair.usersOfPair[1 - b]) ?
                                               HideMode.NONE :
                                               // If the observed player is sneaking hide him fully
                                               (currentPair.usersOfPair[1 - b].getPlayer().isSneaking() ?
                                                HideMode.FULL :
                                                HideMode.INFORMATION_ONLY));
                            }
                        });
                    }

                    pairExecutor.shutdown();

                    try
                    {
                        if (!pairExecutor.awaitTermination(updateMillis, TimeUnit.MILLISECONDS))
                        {
                            VerboseSender.getInstance().sendVerboseMessage("Could not finish ESP cycle. Please consider upgrading your hardware or increasing the update_ticks option in the config if this message appears in large quantities.", false, true);
                        }
                    } catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                    // Update_Ticks: the refresh-rate of the check.
                }, 0L, updateTicks);
    }

    /**
     * Determines if two {@link User}s can see each other.
     */
    private boolean canSee(User observerUser, User watchedUser)
    {
        final Player observer = observerUser.getPlayer();
        final Player watched = watchedUser.getPlayer();

        // ------------------------------------- Glowing ------------------------------------ //
        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
                break;
            case MC111:
            case MC112:
            case MC113:
                if (watched.hasPotionEffect(PotionEffectType.GLOWING))
                {
                    return true;
                }
                break;
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }

        // ----------------------------------- Calculation ---------------------------------- //

        // Not bypassed
        if (observerUser.isBypassed(this.getModuleType()) ||
            // Has not logged in recently to prevent bugs
            observerUser.getLoginData().recentlyUpdated(0, 3000))
        {
            return true;
        }

        //canSee = observer.hasLineOfSight(watched);
        final Vector[] cameraVectors = VectorUtils.getCameraVectors(observer);

        // Get the Vectors of the hitbox to check.
        final Vector[] watchedHitboxVectors = (watched.isSneaking() ?
                                               Hitbox.ESP_SNEAKING_PLAYER :
                                               Hitbox.ESP_PLAYER).getCalculationVectors(watched.getLocation(), true);

        // The distance of the intersections in the same block is equal as of the
        // BlockIterator mechanics.
        final Set<Double> lastIntersectionsCache = new HashSet<>();

        for (Vector cameraVector : cameraVectors)
        {
            for (final Vector destinationVector : watchedHitboxVectors)
            {
                final Location start = cameraVector.toLocation(observer.getWorld());
                // The resulting Vector
                // The camera is not blocked by non-solid blocks
                // Vector is intersecting with some blocks
                //
                // Cloning IS needed as we are in a second loop.
                final Vector between = destinationVector.clone().subtract(cameraVector);

                // ---------------------------------------------- FOV ----------------------------------------------- //
                final Vector cameraRotation = cameraVector.clone().subtract(observer.getLocation().toVector());

                if (cameraRotation.angle(between) > MAX_FOV)
                {
                    continue;
                }

                // ---------------------------------------- Cache Calculation --------------------------------------- //

                // Make sure the chunks are loaded.
                if (!ChunkUtils.areChunksLoadedBetweenLocations(start, start.clone().add(between)))
                {
                    // If the chunks are not loaded assume the players can see each other.
                    return true;
                }

                boolean cacheHit = false;

                Location cacheLocation;
                for (Double length : lastIntersectionsCache)
                {
                    cacheLocation = start.clone().add(between.clone().normalize().multiply(length));

                    // Not yet cached.
                    if (length == 0)
                    {
                        continue;
                    }

                    final Material type = cacheLocation.getBlock().getType();

                    if (BlockUtils.isReallyOccluding(type) && type.isSolid())
                    {
                        cacheHit = true;
                        break;
                    }
                }

                if (cacheHit)
                {
                    continue;
                }

                // --------------------------------------- Normal Calculation --------------------------------------- //

                final double intersect = VectorUtils.getDistanceToFirstIntersectionWithBlock(start, between);

                // No intersection found
                if (intersect == 0)
                {
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
        if (event.getNewGameMode() == GameMode.SPECTATOR)
        {
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            // Not bypassed
            if (User.isUserInvalid(user, this.getModuleType()))
            {
                return;
            }

            // Manually clear the EspInformationData for better performance.
            for (Player hiddenPlayer : user.getEspInformationData().hiddenPlayers.keySet())
            {
                informationOnlyHider.unModifyInformation(user.getPlayer(), hiddenPlayer);
                fullHider.unModifyInformation(user.getPlayer(), hiddenPlayer);
            }
            user.getEspInformationData().hiddenPlayers.clear();
        }
    }

    private void updatePairHideMode(final Pair pair, final HideMode hideMode)
    {
        updateHideMode(pair.usersOfPair[0], pair.usersOfPair[1].getPlayer(), hideMode);
        updateHideMode(pair.usersOfPair[1], pair.usersOfPair[0].getPlayer(), hideMode);
    }

    // No need to synchronize hiddenPlayers as it is accessed in a synchronized task.
    private void updateHideMode(final User observer, final Player watched, final HideMode hideMode)
    {
        // unModifyInformation and modifyInformation are not thread-safe.
        Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> {
            // Observer might have left by now.
            if (observer != null &&
                // Doesn't need to update anything.
                observer.getEspInformationData().hiddenPlayers.get(watched) != hideMode)
            {
                switch (hideMode)
                {
                    case FULL:
                        observer.getEspInformationData().hiddenPlayers.put(watched, HideMode.FULL);
                        // FULL: fullHider active, informationOnlyHider inactive
                        informationOnlyHider.unModifyInformation(observer.getPlayer(), watched);
                        fullHider.modifyInformation(observer.getPlayer(), watched);
                        break;
                    case INFORMATION_ONLY:
                        observer.getEspInformationData().hiddenPlayers.put(watched, HideMode.INFORMATION_ONLY);

                        // INFORMATION_ONLY: fullHider inactive, informationOnlyHider active
                        informationOnlyHider.modifyInformation(observer.getPlayer(), watched);
                        fullHider.unModifyInformation(observer.getPlayer(), watched);
                        break;
                    case NONE:
                        observer.getEspInformationData().hiddenPlayers.remove(watched);

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

    private static class Pair
    {
        final User[] usersOfPair;

        Pair(final User a, final User b)
        {
            usersOfPair = new User[]{
                    a,
                    b
            };
        }

        @Override
        public boolean equals(final Object o)
        {
            if (this == o)
            {
                return true;
            }

            if (o == null || getClass() != o.getClass())
            {
                return false;
            }

            // The other object
            final Pair pair = (Pair) o;
            return (usersOfPair[0].getPlayer().getUniqueId().equals(pair.usersOfPair[0].getPlayer().getUniqueId()) || usersOfPair[0].getPlayer().getUniqueId().equals(pair.usersOfPair[1].getPlayer().getUniqueId())) &&
                   (usersOfPair[1].getPlayer().getUniqueId().equals(pair.usersOfPair[1].getPlayer().getUniqueId()) || usersOfPair[1].getPlayer().getUniqueId().equals(pair.usersOfPair[0].getPlayer().getUniqueId()));
        }

        @Override
        public int hashCode()
        {
            return usersOfPair[0].getPlayer().getUniqueId().hashCode() + usersOfPair[1].getPlayer().getUniqueId().hashCode();
        }
    }
}