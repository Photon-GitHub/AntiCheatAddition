package de.photon.anticheataddition.modules.checks.skinblinker;

import com.comphenix.protocol.PacketType;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import lombok.val;

public final class SkinBlinkerSprinting extends ViolationModule
{
    public static final SkinBlinkerSprinting INSTANCE = new SkinBlinkerSprinting();

    private SkinBlinkerSprinting() {super("Skinblinker.parts.Sprinting");}

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .addPacketListeners(PacketAdapterBuilder.of(this, PacketType.Play.Client.SETTINGS).onReceiving((event, user) -> {
                               /*
                                * An unmodified client can only send such packets if the player is in the menu
                                * -> They obviously cannot sprint or sneak while doing this.
                                * -> They can move, especially in MC 1.9+ because of entity-collision, etc.
                                * -> As of the render-debug-cycle which can be done in the game (F3 + F) I need to check for the change of the skin.
                                */
                               val newSkinComponents = event.getPacket().getIntegers().readSafely(1);

                               // Sprinting or sneaking (detection)
                               if ((event.getPlayer().isSprinting() || event.getPlayer().isSneaking())
                                   // updateSkinComponents returns true if the skin has changed.
                                   && user.updateSkinComponents(newSkinComponents))
                               {
                                   getManagement().flag(Flag.of(user).setAddedVl(50));
                               }
                           }).build())
                           .build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(200, 25).build();
    }
}
