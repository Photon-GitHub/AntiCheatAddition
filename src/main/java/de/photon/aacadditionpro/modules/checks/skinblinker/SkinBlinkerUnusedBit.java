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
                                * Check for the special 0x80 bit in the skin packet that is officially unused by the protocol and set to 0 in vanilla clients.
                                * Some custom clients like LabyMod use that bit for their cosmetics.
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