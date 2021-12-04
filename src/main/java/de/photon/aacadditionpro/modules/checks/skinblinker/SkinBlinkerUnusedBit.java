package de.photon.aacadditionpro.modules.checks.skinblinker;

import com.comphenix.protocol.PacketType;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.protocol.PacketAdapterBuilder;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import lombok.val;

public class SkinBlinkerUnusedBit extends ViolationModule
{
    public SkinBlinkerUnusedBit()
    {
        super("Skinblinker.parts.UnusedBit");
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .addPacketListeners(PacketAdapterBuilder.of(PacketType.Play.Client.SETTINGS).onReceiving(event -> {
                               /*
                                * A unmodified client can only send such packets if the player is in the menu
                                * -> he obviously cannot Sprint or Sneak when doing this.
                                * -> he can move, especially in MC 1.9 and upward because of entity-collision, etc.
                                * -> As of the render-debug-cycle which can be done in the game (F3 + F) I need to check for the change of the skin.
                                */
                               val user = User.safeGetUserFromPacketEvent(event);
                               if (User.isUserInvalid(user, this)) return;

                               val newSkinComponents = event.getPacket().getIntegers().readSafely(1);

                               // Unused skin bit used (detection)
                               if ((newSkinComponents & 0x80) != 0) getManagement().flag(Flag.of(user).setAddedVl(100));
                           }).build())
                           .build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(200, 50).build();
    }
}