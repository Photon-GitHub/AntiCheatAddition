package de.photon.anticheataddition.modules.checks.packetanalysis;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.subdata.SlotSelectionData;
import de.photon.anticheataddition.util.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

/**
 * Vanilla never sends a HELD_ITEM_CHANGE if the slot is the same as before.
 */
public final class PacketAnalysisDuplicateHeldSlot extends ViolationModule
{
    public static final PacketAnalysisDuplicateHeldSlot INSTANCE = new PacketAnalysisDuplicateHeldSlot();

    private PacketAnalysisDuplicateHeldSlot()
    {
        super("PacketAnalysis.parts.DuplicateHeldSlot");
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.of(this, PacketAdapterBuilder.of(this, PacketType.Play.Client.HELD_ITEM_CHANGE, PacketType.Play.Server.HELD_ITEM_CHANGE, PacketType.Play.Server.JOIN_GAME, PacketType.Play.Server.RESPAWN)
                                                         .priority(PacketListenerPriority.MONITOR)
                                                         .onReceivingRaw(this::onPacketReceive)
                                                         .onSendingRaw(this::onPacketSend)
                                                         .build());
    }

    private void onPacketSend(PacketSendEvent event)
    {
        final User user = User.getUser(event);
        if (user == null) return;
        final SlotSelectionData data = user.getData().object.packetAnalysisSlotSelection;
        if (User.isUserInvalid(user, this)) {
            data.reset();
            return;
        }
        event.getPostTasks().add(() -> {
            if (!event.isCancelled()) data.reset();
        });
    }

    private void onPacketReceive(PacketReceiveEvent event)
    {
        final User user = User.getUser(event);
        if (user == null) return;
        final SlotSelectionData data = user.getData().object.packetAnalysisSlotSelection;
        if (User.isUserInvalid(user, this)) {
            data.reset();
            return;
        }

        final int slot = new WrapperPlayClientHeldItemChange(event).getSlot();
        // Invalid selections cannot change vanilla's cached slot, and cancellation cannot undo a client selection.
        if (PacketAnalysisHeldSlot.invalidHeldSlot(slot)) return;
        if (data.record(slot))
            getManagement().flag(Flag.of(user).setAddedVl(10).setDebug(() -> "PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " selected hotbar slot " + slot + " twice without a slot change."));
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).emptyThresholdManagement().withDecay(200, 2).build();
    }
}
