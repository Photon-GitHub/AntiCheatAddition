package de.photon.aacadditionproold.modules.checks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.aacadditionproold.AACAdditionPro;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.modules.PacketListenerModule;
import de.photon.aacadditionproold.modules.ViolationModule;
import de.photon.aacadditionproold.user.User;
import de.photon.aacadditionproold.user.UserManager;
import de.photon.aacadditionproold.util.violationlevels.ViolationLevelManagement;
import lombok.Getter;

public class SkinBlinker extends PacketAdapter implements PacketListenerModule, ViolationModule
{
    @Getter
    private static final SkinBlinker instance = new SkinBlinker();

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
        final User user = UserManager.safeGetUserFromPacketEvent(event);

        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        // Sprinting or sneaking (detection)
        if ((event.getPlayer().isSprinting() || event.getPlayer().isSneaking())) {
            final int newSkinComponents = event.getPacket().getIntegers().readSafely(1);

            // updateSkinComponents returns true if the skin has changed.
            if (user.updateSkinComponents(newSkinComponents)) {
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
    public boolean isSubModule()
    {
        return false;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.SKINBLINKER;
    }
}