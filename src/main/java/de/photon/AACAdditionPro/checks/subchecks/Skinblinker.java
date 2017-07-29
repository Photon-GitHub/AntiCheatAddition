package de.photon.AACAdditionPro.checks.subchecks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.storage.management.ViolationLevelManagement;

public class Skinblinker extends PacketAdapter implements AACAdditionProCheck
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getAdditionHackType(), 100);

    public Skinblinker()
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
        if (AACAdditionProCheck.isUserInvalid(user)) {
            return;
        }

        //Sprinting or sneaking (detection)
        if (user.getPlayer().isSprinting() || user.getPlayer().isSneaking())
        {
            if (user.getSettingsData().skin_components == 0) {
                user.getSettingsData().skin_components = event.getPacket().getIntegers().readSafely(1);
            } else if (event.getPacket().getIntegers().readSafely(1) != user.getSettingsData().skin_components) {
                vlManager.flag(user.getPlayer(), -1, () -> {}, () -> {});
                user.getSettingsData().skin_components = event.getPacket().getIntegers().readSafely(1);
            }
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
        return AdditionHackType.SKINBLINKER;
    }
}