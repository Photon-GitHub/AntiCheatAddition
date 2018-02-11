package de.photon.AACAdditionPro.checks.subchecks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;

public class SkinBlinker extends PacketAdapter implements ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 100);

    public SkinBlinker()
    {
        super(AACAdditionPro.getInstance(), PacketType.Play.Client.SETTINGS);
    }

    @Override
    public void onPacketReceiving(final PacketEvent event)
    {
        /*
         * A unmodified client can only send such packets if the player is in the menu
         * -> he obviously cannot Sprint or Sneak when doing this.
         * -> he can move, especially in MC 1.9 and upward because of entity-collision, etc.
         * -> As of the render-debug-cycle which can be done in the game (F3 + F) I need to check for the change of the skin.
         */

        // Sprinting or sneaking (detection)
        if (event.getPlayer().isSprinting() || event.getPlayer().isSneaking())
        {
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            // Not bypassed
            if (User.isUserInvalid(user))
            {
                return;
            }

            final int newSkinComponents = event.getPacket().getIntegers().readSafely(1);

            // In the beginning oldSkinComponents is 0.
            if (user.getSkinData().skinComponents != 0 &&
                // Actual detection.
                newSkinComponents != user.getSkinData().skinComponents)
            {
                vlManager.flag(user.getPlayer(), -1, () -> {}, () -> {});
            }

            // There is no need to update the skinComponents when not sprinting / sneaking as this is only
            // unnecessary writing and the detection doesn't need the additional info. (At max 1 false vl)
            user.getSkinData().skinComponents = newSkinComponents;
        }
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.SKINBLINKER;
    }
}