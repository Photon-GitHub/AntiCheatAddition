package de.photon.anticheataddition.modules.checks.skinblinker;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSettings;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.util.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

public final class SkinBlinkerSprinting extends ViolationModule
{
    public static final SkinBlinkerSprinting INSTANCE = new SkinBlinkerSprinting();

    private SkinBlinkerSprinting() {super("Skinblinker.parts.Sprinting");}

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .addPacketListeners(PacketAdapterBuilder.of(this, PacketType.Play.Client.CLIENT_SETTINGS).onReceiving((event, user) -> {
                               /*
                                * An unmodified client can only send such packets if the player is in the menu
                                * -> They obviously cannot sprint or sneak while doing this.
                                * -> They can move, especially in MC 1.9+ because of entity-collision, etc.
                                * -> As of the render-debug-cycle which can be done in the game (F3 + F) I need to check for the change of the skin.
                                */
                               final WrapperPlayClientSettings settings = new WrapperPlayClientSettings(event);
                               final byte skinMask = settings.getSkinMask();

                               // Sprinting or sneaking (detection)
                               if ((user.getPlayer().isSprinting() || user.getPlayer().isSneaking())
                                   // updateSkinComponents returns true if the skin has changed.
                                   && user.updateSkinComponents(skinMask)) {
                                   getManagement().flag(Flag.of(user)
                                                            .setAddedVl(50)
                                                            .setDebug(() -> "SkinBlinkerSprinting: Skin changed while sprinting or sneaking. New skinmask is %x".formatted(skinMask)));
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
