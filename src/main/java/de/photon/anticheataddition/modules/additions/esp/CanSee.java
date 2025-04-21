package de.photon.anticheataddition.modules.additions.esp;
import de.photon.anticheataddition.util.minecraft.world.entity.InternalPotion;
import org.bukkit.entity.Player;

/**
 * Defines visibility checking functionality for ESP detection.
 * Determines if one player can see another player, considering game mechanics like
 * potion effects and camera perspectives.
 */
public interface CanSee {
    /**
     * The implementation to use based on configuration settings.
     */
    CanSee INSTANCE = Esp.INSTANCE.loadBoolean(".calculate_third_person_modes", false)
            ? new ThirdPersonCameraCanSee()
            : new SingleCameraCanSee();

    /**
     * Maximum distance squared that a player with Blindness can see
     */
    double MAX_BLINDNESS_DISTANCE_SQUARED = 5.5 * 5.5;

    /**
     * Maximum distance squared that a player with Darkness can see
     */
    double MAX_DARKNESS_DISTANCE_SQUARED = 15.5 * 15.5;

    /**
     * Determines if an observer can see a watched player, considering potion effects
     * and environment conditions.
     *
     * @param observer The player who is looking
     * @param watched The player being looked at
     * @return true if the observer can see the watched player, false otherwise
     */
    static boolean canSee(Player observer, Player watched) {
        // If the watched player has glowing effect, they're always visible
        if (InternalPotion.GLOWING.hasPotionEffect(watched)) {
            return true;
        }

        // Check visibility limitations from potion effects
        double distanceSquared = observer.getLocation().distanceSquared(watched.getLocation());
        if (InternalPotion.BLINDNESS.hasPotionEffect(observer) &&
                distanceSquared > MAX_BLINDNESS_DISTANCE_SQUARED) {
            return false;
        }

        if (InternalPotion.DARKNESS.hasPotionEffect(observer) &&
                distanceSquared > MAX_DARKNESS_DISTANCE_SQUARED) {
            return false;
        }

        // Delegate to the appropriate implementation for detailed visibility check
        return INSTANCE.canSeeTracing(observer, watched);
    }

    /**
     * Performs a detailed ray-tracing check to determine if one player can see another.
     *
     * @param observer The player who is looking
     * @param watched The player being looked at
     * @return true if the observer can see the watched player, false otherwise
     */
    boolean canSeeTracing(Player observer, Player watched);
}