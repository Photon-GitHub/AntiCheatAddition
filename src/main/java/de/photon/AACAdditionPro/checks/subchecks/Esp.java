package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.ServerVersion;
import de.photon.AACAdditionPro.checks.ViolationModule;
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
import de.photon.AACAdditionPro.util.world.ChunkUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Esp implements ViolationModule
{
    // The auto-config-data
    private double renderDistanceSquared = 0;
    private boolean hideAfterRenderDistance = true;

    private int updateMillis;

    // The camera offset for 3rd person
    private static final double THIRD_PERSON_OFFSET = 5D;

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
    public ModuleType getModuleType()
    {
        return ModuleType.ESP;
    }

    @Override
    public void subEnable()
    {
        // ---------------------------------------------------- Auto-configuration ----------------------------------------------------- //
        final int updateTicks = AACAdditionPro.getInstance().getConfig().getInt(this.getConfigString() + ".update_ticks");
        updateMillis = 50 * updateTicks;

        final YamlConfiguration spigot = Configs.SPIGOT.getConfigurationRepresentation().getYamlConfiguration();
        final ConfigurationSection worlds = spigot.getConfigurationSection("world-settings");

        for (final String world : worlds.getKeys(false))
        {
            int currentPlayerTrackingRange = spigot.getInt(worlds.getCurrentPath() + "." + world + ".entity-tracking-range.players");

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
                    final List<User> users = new ArrayList<>(UserManager.getUsersUnwrapped());

                    // Iterate through all player-constellations
                    User observingUser;
                    while (!users.isEmpty())
                    {
                        /*
                            Remove the finished player to reduce the amount of added entries.
                            This makes sure the player won't have a connection with himself.
                            Remove index - 1 for the best performance.
                        */
                        observingUser = users.remove(users.size() - 1);

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
                                final Player observer = currentPair.usersOfPair[b].getPlayer();
                                final Player watched = currentPair.usersOfPair[1 - b].getPlayer();

                                // ------------------------- Can one Player see the other ? ------------------------- //
                                boolean canSee;

                                // ------------------------------------- Glowing ------------------------------------ //
                                switch (ServerVersion.getActiveServerVersion())
                                {
                                    case MC188:
                                        canSee = false;
                                        break;
                                    case MC111:
                                    case MC112:
                                    case MC113:
                                        canSee = watched.hasPotionEffect(PotionEffectType.GLOWING);
                                        break;
                                    default:
                                        throw new IllegalStateException("Unknown minecraft version");
                                }

                                // Not already able to see (due to e.g. glowing)
                                if (!canSee &&
                                    // Not bypassed
                                    !currentPair.usersOfPair[b].isBypassed() &&
                                    // Has not logged in recently to prevent bugs
                                    !currentPair.usersOfPair[b].getLoginData().recentlyUpdated(0, 3000))
                                {
                                    //canSee = observer.hasLineOfSight(watched);
                                    final Vector[] cameraVectors = getCameraVectors(observer);

                                    // Get the Vectors of the hitbox to check.
                                    final Vector[] watchedHitboxVectors = (watched.isSneaking() ?
                                                                           Hitbox.ESP_SNEAKING_PLAYER :
                                                                           Hitbox.ESP_PLAYER).getCalculationVectors(watched.getLocation(), true);


                                    // The distance of the intersections in the same block is equal as of the
                                    // BlockIterator mechanics.
                                    final Set<Double> lastIntersectionsCache = ConcurrentHashMap.newKeySet();

                                    for (int i = 0; i < cameraVectors.length; i++)
                                    {
                                        for (final Vector calculationVector : watchedHitboxVectors)
                                        {
                                            final Location start = cameraVectors[i].toLocation(observer.getWorld());
                                            // The resulting Vector
                                            // The camera is not blocked by non-solid blocks
                                            // Vector is intersecting with some blocks
                                            //
                                            // No cloning is needed here as the calculationVector is only used once.
                                            final Vector between = calculationVector.subtract(cameraVectors[i]);

                                            // ---------------------------------------------- FOV ----------------------------------------------- //
                                            final Vector cameraRotation = observer.getLocation().getDirection();

                                            // Exactly the opposite rotation for the front-view
                                            if (i == 1)
                                                cameraRotation.multiply(-1);

                                            if (cameraRotation.angle(between) > MAX_FOV)
                                                continue;

                                            // --------------------------------------- Normal Calculation --------------------------------------- //

                                            boolean cacheHit = false;
                                            for (Double length : lastIntersectionsCache)
                                            {
                                                // Not yet cached.
                                                if (length == 0)
                                                    continue;

                                                if (VectorUtils.vectorIntersectsWithBlockAt(start, between, length))
                                                {
                                                    cacheHit = true;
                                                }
                                            }

                                            if (cacheHit)
                                                continue;

                                            // Make sure the chunks are loaded.
                                            if (ChunkUtils.areChunksLoadedBetweenLocations(start, start.clone().add(between)))
                                            {
                                                final double intersect = VectorUtils.getDistanceToFirstIntersectionWithBlock(start, between);

                                                // No intersection found
                                                if (intersect == 0)
                                                {
                                                    canSee = true;
                                                    break;
                                                }

                                                lastIntersectionsCache.add(intersect);
                                            }
                                            // If the chunks are not loaded assume the players can see each other.
                                            else
                                            {
                                                canSee = true;
                                                break;
                                            }
                                        }
                                    }

                                    // No need to further calculate anything as the player can already be seen.
                                    if (canSee)
                                        break;


                                    // Low probability to help after the camera view was changed. -> clearing
                                    lastIntersectionsCache.clear();
                                }
                                else
                                {
                                    canSee = true;
                                }

                                updateHideMode(currentPair.usersOfPair[b], currentPair.usersOfPair[1 - b].getPlayer(),
                                               // Is the user visible
                                               canSee ?
                                               HideMode.NONE :
                                               // If the observed player is sneaking hide him fully
                                               (currentPair.usersOfPair[1 - b].getPlayer().isSneaking() ?
                                                HideMode.FULL :
                                                HideMode.INFORMATION_ONLY));

                                // No special HideMode here as of the players being in 2 different worlds to decrease CPU load.
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

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event)
    {
        if (event.getNewGameMode() == GameMode.SPECTATOR)
        {
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            // Not bypassed
            if (User.isUserInvalid(user))
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

    /**
     * @return an array of {@link Vector}s which represent the 3 different camera modes in minecraft, 1st person and the two
     * 3rd person views.
     */
    private static Vector[] getCameraVectors(final Player player)
    {
        /*
            All the vectors
            [0] = normal (eyeposition vector)
            [1] = front
            [2] = behind
        */
        final Vector[] vectors = new Vector[3];

        // Front vector : The 3rd person perspective in front of the player
        // Use THIRD_PERSON_OFFSET to get the maximum positions
        vectors[1] = player.getLocation().getDirection().clone().normalize().multiply(THIRD_PERSON_OFFSET);

        // Behind vector : The 3rd person perspective behind the player
        vectors[2] = vectors[1].clone().multiply(-1);

        final Location eyeLocation = player.getEyeLocation();

        // Normal
        vectors[0] = eyeLocation.toVector();

        // Do the Cameras intersect with Blocks
        // Get the length of the first intersection or 0 if there is none

        // [0] = frontIntersection
        // [1] = behindIntersection
        final double[] intersections = new double[]{
                VectorUtils.getDistanceToFirstIntersectionWithBlock(eyeLocation, vectors[1]),
                VectorUtils.getDistanceToFirstIntersectionWithBlock(eyeLocation, vectors[2])
        };

        for (int i = 0; i < intersections.length; i++)
        {
            // There is an intersection
            if (intersections[i] != 0)
            {
                // Now we need to make sure the vectors are not inside of blocks as the method above returns.
                // The 0.05 factor makes sure that we are outside of the block and not on the edge.
                intersections[i] -= 0.05 + (0.5 / Math.sin(vectors[i + 1].angle(vectors[i + 1].clone().setY(0))));
                // Add the correct position.
                vectors[i + 1].normalize().multiply(intersections[i]);
            }
        }

        return vectors;
    }

    private void updatePairHideMode(final Pair pair, final HideMode hideMode)
    {
        updateHideMode(pair.usersOfPair[0], pair.usersOfPair[1].getPlayer(), hideMode);
        updateHideMode(pair.usersOfPair[1], pair.usersOfPair[0].getPlayer(), hideMode);
    }

    private synchronized void updateHideMode(final User observer, final Player object, final HideMode hideMode)
    {
        // unModifyInformation and modifyInformation are not thread-safe.
        Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> {
            if (observer.getEspInformationData().hiddenPlayers.get(object) != hideMode)
            {
                switch (hideMode)
                {
                    case FULL:
                        observer.getEspInformationData().hiddenPlayers.put(object, HideMode.FULL);
                        // FULL: fullHider active, informationOnlyHider inactive
                        informationOnlyHider.unModifyInformation(observer.getPlayer(), object);
                        fullHider.modifyInformation(observer.getPlayer(), object);
                        break;
                    case INFORMATION_ONLY:
                        observer.getEspInformationData().hiddenPlayers.put(object, HideMode.INFORMATION_ONLY);

                        // INFORMATION_ONLY: fullHider inactive, informationOnlyHider active
                        informationOnlyHider.modifyInformation(observer.getPlayer(), object);
                        fullHider.unModifyInformation(observer.getPlayer(), object);
                        break;
                    case NONE:
                        observer.getEspInformationData().hiddenPlayers.remove(object);

                        // NONE: fullHider inactive, informationOnlyHider inactive
                        informationOnlyHider.unModifyInformation(observer.getPlayer(), object);
                        fullHider.unModifyInformation(observer.getPlayer(), object);
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
                   (usersOfPair[1].getPlayer().getUniqueId().equals(pair.usersOfPair[1].getPlayer().getUniqueId()) || usersOfPair[1].getPlayer().getUniqueId().equals(pair.usersOfPair[0].getPlayer().getUniqueId())
                   );
        }

        @Override
        public int hashCode()
        {
            return usersOfPair[0].getPlayer().getUniqueId().hashCode() + usersOfPair[1].getPlayer().getUniqueId().hashCode();
        }
    }
}