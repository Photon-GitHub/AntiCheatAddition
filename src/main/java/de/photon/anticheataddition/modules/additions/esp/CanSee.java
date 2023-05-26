package de.photon.anticheataddition.modules.additions.esp;

import de.photon.anticheataddition.util.minecraft.world.InternalPotion;
import org.bukkit.entity.Player;

public interface CanSee
{
    CanSee INSTANCE = Esp.INSTANCE.loadBoolean(".calculate_third_person_modes", false) ? new ThirdPersonCameraCanSee() : new SingleCameraCanSee();

    double BLINDNESS_DISTANCE = 5.5 * 5.5;
    double DARKNESS_DISTANCE = 15.5 * 15.5;

    default boolean canSee(Player observer, Player watched)
    {
        if (InternalPotion.GLOWING.hasPotionEffect(watched)) return true;

        if (InternalPotion.BLINDNESS.hasPotionEffect(observer) && observer.getLocation().distanceSquared(watched.getLocation()) > BLINDNESS_DISTANCE) return false;
        if (InternalPotion.DARKNESS.hasPotionEffect(observer) && observer.getLocation().distanceSquared(watched.getLocation()) > DARKNESS_DISTANCE) return false;

        return INSTANCE.canSeeTracing(observer, watched);
    }

    boolean canSeeTracing(Player observer, Player watched);
}
