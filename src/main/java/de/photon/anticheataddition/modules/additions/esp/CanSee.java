package de.photon.anticheataddition.modules.additions.esp;

import de.photon.anticheataddition.util.minecraft.world.entity.InternalPotion;
import org.bukkit.entity.Player;

public interface CanSee
{
    CanSee INSTANCE = Esp.INSTANCE.loadBoolean(".calculate_third_person_modes", false) ? new ThirdPersonCameraCanSee() : new SingleCameraCanSee();

    double BLINDNESS_DISTANCE = 5.5 * 5.5;
    double DARKNESS_DISTANCE = 15.5 * 15.5;

    static boolean canSee(Player observer, Player watched)
    {
        if (InternalPotion.GLOWING.hasPotionEffect(watched)) return true;

        final double distanceSquared = observer.getLocation().distanceSquared(watched.getLocation());

        if (distanceSquared < 1) return true;
        if (InternalPotion.BLINDNESS.hasPotionEffect(observer) && distanceSquared > BLINDNESS_DISTANCE) return false;
        if (InternalPotion.DARKNESS.hasPotionEffect(observer) && distanceSquared > DARKNESS_DISTANCE) return false;

        return INSTANCE.canSeeTracing(observer, watched);
    }

    boolean canSeeTracing(Player observer, Player watched);
}
