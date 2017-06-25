package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.storage.management.ViolationLevelManagement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;

public class BlindnessSprint implements Listener, AACAdditionProCheck
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getAdditionHackType(), 100L);

    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancel_vl;

    @EventHandler
    public void on(final PlayerMoveEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (user == null || user.isBypassed()) {
            return;
        }

        // No flying check as Blindness also applies to the flying behaviour
        // The player has Blindness
        if (user.getPlayer().hasPotionEffect(PotionEffectType.BLINDNESS) &&
            // The player is Sprinting (Detection)
            user.getPlayer().isSprinting() &&
            // Not teleported recently.
            !user.getTeleportData().recentlyUpdated(1000))
        {
            vlManager.flag(user.getPlayer(), cancel_vl, () -> event.getPlayer().teleport(event.getFrom()), () -> {});
        }
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public AdditionHackType getAdditionHackType()
    {
        return AdditionHackType.BLINDNESS_SPRINT;
    }
}