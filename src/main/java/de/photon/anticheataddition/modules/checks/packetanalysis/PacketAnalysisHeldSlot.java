package de.photon.anticheataddition.modules.checks.packetanalysis;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.util.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

/**
 * Checks the hotbar slot domain, without treating repeated selections as cheating.
 */
public final class PacketAnalysisHeldSlot extends ViolationModule
{
    public static final PacketAnalysisHeldSlot INSTANCE = new PacketAnalysisHeldSlot();

    private PacketAnalysisHeldSlot()
    {
        super("PacketAnalysis.parts.HeldSlot");
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.of(this, PacketAdapterBuilder.of(this, PacketType.Play.Client.HELD_ITEM_CHANGE)
                                                         .priority(PacketListenerPriority.LOW)
                                                         .onReceiving((event, user) -> {
                                                             if (event.isCancelled()) return;
                                                             final int slot = new WrapperPlayClientHeldItemChange(event).getSlot();

                                                             if (!invalidHeldSlot(slot)) return;
                                                             getManagement().flag(Flag.of(user).setAddedVl(50).setDebug(() -> "PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " sent an invalid hotbar slot (" + slot + ")."));
                                                         }).build());
    }

    public static boolean invalidHeldSlot(final int slot)
    {
        return slot < 0 || slot > 8;
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).emptyThresholdManagement().withDecay(200, 2).build();
    }
}
