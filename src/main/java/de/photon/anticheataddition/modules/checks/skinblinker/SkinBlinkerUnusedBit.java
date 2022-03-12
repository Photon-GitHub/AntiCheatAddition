package de.photon.anticheataddition.modules.checks.skinblinker;

import com.comphenix.protocol.PacketType;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
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