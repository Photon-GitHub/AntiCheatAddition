package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.mathematics.Hitbox;
import de.photon.AACAdditionPro.util.mathematics.VectorUtils;
import de.photon.AACAdditionPro.util.visibility.HideMode;
import de.photon.AACAdditionPro.util.visibility.PlayerInformationModifier;
import de.photon.AACAdditionPro.util.visibility.informationmodifiers.InformationObfuscator;
import de.photon.AACAdditionPro.util.visibility.informationmodifiers.PlayerHider;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Esp implements AACAdditionProCheck
{

    // The auto-config-data
    private double renderDistanceSquared = 0;
    private boolean hideAfterRenderDistance = true;
    @LoadFromConfiguration(configPath = ".update_ticks")
    private int update_ticks;

    // The camera offset for 3rd person
    private static final double THIRD_PERSON_OFFSET = 5D;

    // The real MAX_FOV is 110 (quake pro), which results in 150° according to tests.
    // 150° + 15° (compensation) = 165°
    private static final double MAX_FOV = Math.toRadians(165D);

    // No structural changes in the evaluation, thus a HashSet can be used.
    private final Set<Pair> playerConnections = new HashSet<>();

    private final PlayerInformationModifier fullHider = new PlayerHider();
    private final PlayerInformationModifier informationOnlyHider = new InformationObfuscator();


    // The task number for Bukkit's internal systems
    private int taskNumber;

    @Override
    public AdditionHackType getAdditionHackType()
    {
        return AdditionHackType.ESP;
    }

    @Override
    public void subEnable()
    {
        // ---------------------------------------------------- Auto-configuration ----------------------------------------------------- //

        final YamlConfiguration spigot = YamlConfiguration.loadConfiguration(new File("spigot.yml"));
        final ConfigurationSection worlds = spigot.getConfigurationSection("world-settings");

        for (final String s : worlds.getKeys(false))
        {
            int currentPlayerTrackingRange = spigot.getInt(worlds.getCurrentPath() + "." + s + ".entity-tracking-range.players");

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
                    //All users
                    final Iterable<User> users = UserManager.getUsers();

                    //Iterate through all player-constellations
                    for (final User observer : users)
                    {
                        // Not a spectator
                        if (observer.getPlayer().getGameMode() != GameMode.SPECTATOR)
                        {
                            // All users can potentially be seen
                            for (final User watched : users)
                            {
                                // The watched player is also not in Spectator mode
                                if (watched.getPlayer().getGameMode() != GameMode.SPECTATOR &&
                                    // The users are not the same
                                    !observer.refersToUUID(watched.getPlayer().getUniqueId()))
                                {
                                    playerConnections.add(new Pair(observer, watched));
                                }
                            }
                        }
                    }


                    // Use as many processors as possible
                    final ExecutorService executorService = Executors.newWorkStealingPool();

                    for (final Pair pair : playerConnections)
                    {
                        executorService.submit(
                                () ->
                                {
                                    // The Users are in the same world
                                    if (pair.usersOfPair[0].getPlayer().getWorld().equals(pair.usersOfPair[1].getPlayer().getWorld()))
                                    {
                                        final double pairDistanceSquared = pair.usersOfPair[0].getPlayer().getLocation().distanceSquared(pair.usersOfPair[1].getPlayer().getLocation());

                                        // Less than 1 block distance
                                        // Everything (smaller than 1)^2 will result in something smaller than 1
                                        if (pairDistanceSquared < 1)
                                        {
                                            updatePairHideMode(pair, HideMode.NONE);
                                            return;
                                        }

                                        if (pairDistanceSquared > renderDistanceSquared)
                                        {
                                            updatePairHideMode(pair, hideAfterRenderDistance ?
                                                                     HideMode.FULL :
                                                                     HideMode.NONE);
                                            return;
                                        }

                                        for (byte b = 0; b <= 1; b++)
                                        {
                                            final Player observer = pair.usersOfPair[b].getPlayer();
                                            final Player watched = pair.usersOfPair[1 - b].getPlayer();

                                            // ------------------------- Can one Player see the other ? ------------------------- //
                                            boolean canSee = false;

                                            // Not bypassed
                                            if (!pair.usersOfPair[b].isBypassed() &&
                                                // Has not logged in recently to prevent bugs
                                                !pair.usersOfPair[b].getLoginData().recentlyUpdated(3000))
                                            {
                                                final Vector[] cameraVectors = getCameraVectors(observer);

                                                final Hitbox hitboxOfWatched = watched.isSneaking() ?
                                                                               Hitbox.SNEAKING_PLAYER :
                                                                               Hitbox.PLAYER;

                                                final Iterable<Vector> watchedHitboxVectors = hitboxOfWatched.getCalculationVectors(watched.getLocation(), true);

                                                double lastIntersectionCache = 1;

                                                for (int i = 0; i < cameraVectors.length; i++)
                                                {
                                                    Vector perspective = cameraVectors[i];

                                                    for (final Vector calculationVector : watchedHitboxVectors)
                                                    {
                                                        //System.out.println("OwnVec: " + watcher.getEyeLocation().toVector() + " |Vector: " + vector);

                                                        final Location start = perspective.toLocation(observer.getWorld());
                                                        // The resulting Vector
                                                        // The camera is not blocked by non-solid blocks
                                                        // Vector is intersecting with some blocks
                                                        final Vector between = calculationVector.clone().subtract(perspective);

                                                        // ---------------------------------------------- FOV ----------------------------------------------- //
                                                        Vector cameraRotation = observer.getLocation().getDirection();

                                                        // Exactly the opposite rotation for the front-view
                                                        if (i == 1)
                                                        {
                                                            cameraRotation.multiply(-1);
                                                        }

                                                        if (cameraRotation.angle(between) > MAX_FOV)
                                                        {
                                                            continue;
                                                        }

                                                        // --------------------------------------- Normal Calculation --------------------------------------- //

                                                        if (VectorUtils.vectorIntersectsWithBlockAt(start, between, lastIntersectionCache))
                                                        {
                                                            continue;
                                                        }

                                                        final double intersect = VectorUtils.getFirstVectorIntersectionWithBlock(start, between);

                                                        // No intersection found
                                                        if (intersect == 0)
                                                        {
                                                            canSee = true;
                                                            break;
                                                        }

                                                        lastIntersectionCache = intersect;
                                                    }
                                                }
                                            }
                                            else
                                            {
                                                canSee = true;
                                            }

                                            if (canSee)
                                            {
                                                // Can see the other player
                                                updateHideMode(pair.usersOfPair[b], pair.usersOfPair[1 - b].getPlayer(), HideMode.NONE);
                                            }
                                            else
                                            {
                                                // Cannot see the other player
                                                updateHideMode(pair.usersOfPair[b], pair.usersOfPair[1 - b].getPlayer(),
                                                               // If the observed player is sneaking hide him fully
                                                               pair.usersOfPair[1 - b].getPlayer().isSneaking() ?
                                                               HideMode.FULL :
                                                               HideMode.INFORMATION_ONLY);
                                            }
                                        }
                                    }
                                    // No special HideMode here as of the players being in 2 different worlds to decrease CPU load.
                                });

                        executorService.shutdown();
                        // 3 ticks
                        try
                        {
                            executorService.awaitTermination(150, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }
                    }

                    // TODO: You neither need to clear the connections every second nor to add all connections again (exception: on the first start)... event usage

                    // Clear the HashSet for a new Run
                    // This allows for better performance as the internal Map only grows once and purges some bugs, like hiding of offline players.
                    playerConnections.clear();

                    // Update_Ticks: the refresh-rate of the check.
                }, 0L, update_ticks);
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event)
    {
        if (event.getNewGameMode() == GameMode.SPECTATOR)
        {
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            // Not bypassed
            if (AACAdditionProCheck.isUserInvalid(user))
            {
                return;
            }

            user.getEspInformationData().hiddenPlayers.keySet().forEach(hiddenPlayer -> updateHideMode(user, hiddenPlayer, HideMode.NONE));
        }
    }


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
        final double frontIntersection = VectorUtils.getFirstVectorIntersectionWithBlock(eyeLocation, vectors[1]);
        final double behindIntersection = VectorUtils.getFirstVectorIntersectionWithBlock(eyeLocation, vectors[2]);

        // There is an intersection in the front-vector
        if (frontIntersection != 0)
        {
            vectors[1].normalize().multiply(frontIntersection);
        }

        // There is an intersection in the behind-vector
        if (behindIntersection != 0)
        {
            vectors[2].normalize().multiply(behindIntersection);
        }

        return vectors;
    }

    private void updatePairHideMode(final Pair pair, final HideMode hideMode)
    {
        updateHideMode(pair.usersOfPair[0], pair.usersOfPair[1].getPlayer(), hideMode);
        updateHideMode(pair.usersOfPair[1], pair.usersOfPair[0].getPlayer(), hideMode);
    }

    private void updateHideMode(final User observer, final Player object, final HideMode hideMode)
    {
        switch (hideMode)
        {
            case FULL:
                if (observer.getEspInformationData().hiddenPlayers.get(object) != HideMode.FULL)
                {
                    observer.getEspInformationData().hiddenPlayers.put(object, HideMode.FULL);

                    // FULL: fullHider active, informationOnlyHider inactive
                    informationOnlyHider.unModifyInformation(observer.getPlayer(), object);
                    fullHider.modifyInformation(observer.getPlayer(), object);
                }
                break;
            case INFORMATION_ONLY:
                if (observer.getEspInformationData().hiddenPlayers.get(object) != HideMode.INFORMATION_ONLY)
                {
                    observer.getEspInformationData().hiddenPlayers.put(object, HideMode.INFORMATION_ONLY);

                    // INFORMATION_ONLY: fullHider inactive, informationOnlyHider active
                    fullHider.unModifyInformation(observer.getPlayer(), object);
                    informationOnlyHider.modifyInformation(observer.getPlayer(), object);
                }
                break;
            case NONE:
                if (observer.getEspInformationData().hiddenPlayers.containsKey(object))
                {
                    observer.getEspInformationData().hiddenPlayers.remove(object);

                    // NONE: fullHider inactive, informationOnlyHider inactive
                    informationOnlyHider.unModifyInformation(observer.getPlayer(), object);
                    fullHider.unModifyInformation(observer.getPlayer(), object);
                }
                break;
        }
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