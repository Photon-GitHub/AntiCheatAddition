package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.mathematics.Hitbox;
import de.photon.AACAdditionPro.util.mathematics.VectorUtils;
import de.photon.AACAdditionPro.util.visibility.HideMode;
import de.photon.AACAdditionPro.util.visibility.informationmodifiers.InformationObfuscator;
import de.photon.AACAdditionPro.util.visibility.informationmodifiers.PlayerHider;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class Esp implements AACAdditionProCheck
{
    // The camera offset for 3rd person
    private static final double thirdPersonOffset = 5D;

    // The hider / censor stuff
    private final PlayerHider fullHider = new PlayerHider();
    private final InformationObfuscator informationOnlyHider = new InformationObfuscator();

    // Remember already-done-calculations
    private final HashSet<Pair> playerConnections = new HashSet<>();

    // Task
    private int taskNumber;

    // The auto-config-data
    private double render_distance_squared = 0;
    private boolean hide_after_render_distance = true;

    @Override
    public AdditionHackType getAdditionHackType()
    {
        return AdditionHackType.ESP;
    }

    @Override
    public void enable()
    {
        // ---------------------------------------------------- Auto-configuration ----------------------------------------------------- //

        final YamlConfiguration spigot = YamlConfiguration.loadConfiguration(new File("spigot.yml"));
        final ConfigurationSection worlds = spigot.getConfigurationSection("world-settings");

        for (final String s : worlds.getKeys(false)) {
            int currentPlayerTrackingRange = spigot.getInt(worlds.getCurrentPath() + "." + s + ".entity-tracking-range.players");

            // Square
            currentPlayerTrackingRange *= currentPlayerTrackingRange;

            if (currentPlayerTrackingRange > render_distance_squared) {
                render_distance_squared = currentPlayerTrackingRange;

                // Do the maths inside here as reading from a file takes longer than calculating this.
                // 19321 == 139^2 as of the maximum range of the block-iterator
                if (render_distance_squared > 19321) {
                    hide_after_render_distance = false;
                    render_distance_squared = 19321;
                    break;
                }
            }
        }

        // ----------------------------------------------------------- Task ------------------------------------------------------------ //

        taskNumber = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                AACAdditionPro.getInstance(),
                () -> {
                    //All users
                    final Collection<User> users = UserManager.getUsers();

                    //Iterate through all player-constellations
                    for (final User observer : users) {
                        // Not bypassed
                        if (!observer.isBypassed() &&
                            // Not logged in recently (to prevent bugs)
                            !observer.getLoginData().recentlyUpdated(3500))
                        {
                            // All users can potentially be seen
                            for (final User watchedPlayer : users) {
                                // The user are not the same
                                if (!observer.refersToUUID(watchedPlayer.getPlayer().getUniqueId())) {
                                    playerConnections.add(new Pair(observer, watchedPlayer));
                                }
                            }
                        }
                    }

                    for (final Pair pair : playerConnections) {
                        // Are the players in the same world
                        if (pair.usersOfPair[0].getPlayer().getLocation().getWorld().equals(pair.usersOfPair[1].getPlayer().getLocation().getWorld()) &&
                            // Are the players inside the render_distance
                            pair.usersOfPair[0].getPlayer().getLocation().distanceSquared(pair.usersOfPair[1].getPlayer().getLocation()) <= render_distance_squared)
                        {
                            /*
                                Spectator mode needs different handling as a spectator can see through walls and can never be seen by other players who are no spectators as well.
                                [0]: User a is a spectator
                                [1]: User b is a spectator
                            */
                            final boolean[] spectatorMode = {
                                    pair.usersOfPair[0].getPlayer().getGameMode() == GameMode.SPECTATOR,
                                    pair.usersOfPair[1].getPlayer().getGameMode() == GameMode.SPECTATOR
                            };

                            // Gamemode 3 handling
                            if (spectatorMode[0] || spectatorMode[1]) {
                                if (spectatorMode[0]) {
                                    updateHideMode(pair.usersOfPair[0], pair.usersOfPair[1].getPlayer(), HideMode.NONE);

                                    // If both players are in spectator mode noone should be hidden -> HideMode.NONE
                                    updateHideMode(pair.usersOfPair[1], pair.usersOfPair[0].getPlayer(), spectatorMode[1] ?
                                                                                                         HideMode.NONE :
                                                                                                         HideMode.FULL);
                                } else {
                                    // spectatorMode[1] must be true here as spectatorMode[0] == false and one of spectatorMode[0] and spectatorMode[1] must be true !
                                    // -> spectatorMode[1] == true; spectatorMode[0] == false
                                    updateHideMode(pair.usersOfPair[1], pair.usersOfPair[0].getPlayer(), HideMode.NONE);
                                    updateHideMode(pair.usersOfPair[0], pair.usersOfPair[1].getPlayer(), HideMode.FULL);
                                }

                                // Normal handling without spectators
                            } else {
                                if (canDirectlySee(pair.usersOfPair[0].getPlayer(), pair.usersOfPair[1].getPlayer())) {
                                    updateHideMode(pair.usersOfPair[0], pair.usersOfPair[1].getPlayer(), HideMode.NONE);
                                    updateHideMode(pair.usersOfPair[1], pair.usersOfPair[0].getPlayer(), HideMode.NONE);
                                } else {
                                    updateHideMode(pair.usersOfPair[1], pair.usersOfPair[0].getPlayer(), pair.usersOfPair[0].getPlayer().isSneaking() ?
                                                                                                         HideMode.FULL :
                                                                                                         HideMode.INFORMATION_ONLY);

                                    updateHideMode(pair.usersOfPair[0], pair.usersOfPair[1].getPlayer(), pair.usersOfPair[1].getPlayer().isSneaking() ?
                                                                                                         HideMode.FULL :
                                                                                                         HideMode.INFORMATION_ONLY);
                                }
                            }

                            // Players cannot see each other
                        } else if (hide_after_render_distance) {
                            updateHideMode(pair.usersOfPair[1], pair.usersOfPair[0].getPlayer(), HideMode.FULL);
                            updateHideMode(pair.usersOfPair[0], pair.usersOfPair[1].getPlayer(), HideMode.FULL);
                        } else {
                            updateHideMode(pair.usersOfPair[0], pair.usersOfPair[1].getPlayer(), HideMode.NONE);
                            updateHideMode(pair.usersOfPair[1], pair.usersOfPair[0].getPlayer(), HideMode.NONE);
                        }
                    }

                    // TODO: You neither need to clear the connections every second nor to add all connections again (exeption: on the first start)... event usage

                    // Clear the HashSet for a new Run
                    // This allows for better performance as the internal Map only grows once.
                    playerConnections.clear();

                    // Update_Ticks: the refresh-rate of the check.
                }, 0L, AACAdditionPro.getInstance().getConfig().getInt(this.getAdditionHackType().getConfigString() + ".update_ticks"));
    }

    @Override
    public void disable()
    {
        Bukkit.getScheduler().cancelTask(taskNumber);
    }

    private void updateHideMode(final User observer, final Player object, final HideMode hideMode)
    {
        switch (hideMode) {
            case FULL:
                if (observer.getEspInformationData().hiddenPlayers.get(object.getUniqueId()) != HideMode.FULL) {
                    observer.getEspInformationData().hiddenPlayers.put(object.getUniqueId(), HideMode.FULL);

                    // FULL: fullHider active, informationOnlyHider inactive
                    informationOnlyHider.unModifyInformation(observer.getPlayer(), object);
                    fullHider.modifyInformation(observer.getPlayer(), object);
                }
                break;
            case INFORMATION_ONLY:
                if (observer.getEspInformationData().hiddenPlayers.get(object.getUniqueId()) != HideMode.INFORMATION_ONLY) {
                    observer.getEspInformationData().hiddenPlayers.put(object.getUniqueId(), HideMode.INFORMATION_ONLY);

                    // INFORMATION_ONLY: fullHider inactive, informationOnlyHider active
                    fullHider.unModifyInformation(observer.getPlayer(), object);
                    informationOnlyHider.modifyInformation(observer.getPlayer(), object);
                }
                break;
            case NONE:
                if (observer.getEspInformationData().hiddenPlayers.containsKey(object.getUniqueId())) {
                    observer.getEspInformationData().hiddenPlayers.remove(object.getUniqueId());

                    // NONE: fullHider inactive, informationOnlyHider inactive
                    informationOnlyHider.unModifyInformation(observer.getPlayer(), object);
                    fullHider.unModifyInformation(observer.getPlayer(), object);
                }
                break;
        }
    }

    private static boolean canDirectlySee(final Player watcher, final Player object)
    {
        // Not the same world
        if (!watcher.getWorld().equals(object.getWorld())) {
            return false;
        }

        // Less than 1 block distance
        // Everything (smaller than 1)^2 will result in something smaller than 1
        if (watcher.getLocation().distanceSquared(object.getLocation()) < 1) {
            return true;
        }

        final Vector[] camera_vectors = getCameraVectors(watcher);

        final Hitbox usedHitbox = object.isSneaking() ?
                                  Hitbox.SNEAKING_PLAYER :
                                  Hitbox.PLAYER;
        final List<Vector> calc_vectors = usedHitbox.getCalculationVectors(object.getLocation(), true);

        double lastIntersectionCache = 1;

        for (final Vector perspective : camera_vectors) {
            for (final Vector calculationVector : calc_vectors) {
                //System.out.println("OwnVec: " + watcher.getEyeLocation().toVector() + " |Vector: " + vector);

                final Location start = perspective.toLocation(watcher.getWorld());
                // The resulting Vector
                // The camera is not blocked by non-solid blocks
                // Vector is intersecting with some blocks
                final Vector between = calculationVector.clone().subtract(perspective);

                if (VectorUtils.vectorIntersectsWithBlockAt(start, between, lastIntersectionCache)) {
                    continue;
                }

                final double intersect = VectorUtils.getFirstVectorIntersectionWithBlock(start, between);

                if (intersect == 0) {
                    return true;
                }

                lastIntersectionCache = intersect;
            }
        }

        return false;
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
        // Use thirdPersonOffset to get the maximum positions
        vectors[1] = player.getLocation().getDirection().clone().normalize().multiply(thirdPersonOffset);

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
        if (frontIntersection != 0) {
            vectors[1].normalize().multiply(frontIntersection);
        }

        // There is an intersection in the behind-vector
        if (behindIntersection != 0) {
            vectors[2].normalize().multiply(behindIntersection);
        }

        return vectors;
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
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
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