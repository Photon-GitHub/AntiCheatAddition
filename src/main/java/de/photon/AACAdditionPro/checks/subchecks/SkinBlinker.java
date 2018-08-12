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

        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType()))
        {
            return;
        }

        // Has not recently logged in to prevent problems with ProtocolLib's temporary players.
        if (!user.getLoginData().recentlyUpdated(0, 1000) &&
            // Sprinting or sneaking (detection)
            (event.getPlayer().isSprinting() || event.getPlayer().isSneaking()))
        {
            final int newSkinComponents = event.getPacket().getIntegers().readSafely(1);

            // updateSkinComponents returns true if the skin has changed.
            if (user.getSkinData().updateSkinComponents(newSkinComponents))
            {
                vlManager.flag(user.getPlayer(), -1, () -> {}, () -> {});
            }
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