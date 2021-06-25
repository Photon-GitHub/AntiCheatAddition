package de.photon.aacadditionpro.modules.checks.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ModulePacketAdapter;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.messaging.DebugSender;
import de.photon.aacadditionpro.util.packetwrappers.sentbyclient.IWrapperPlayClientLook;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import lombok.val;

public class InventoryRotation extends ViolationModule
{
    @LoadFromConfiguration(configPath = ".teleport_bypass_time")
    private int teleportTime;
    @LoadFromConfiguration(configPath = ".world_change_bypass_time")
    private int worldChangeTime;

    public InventoryRotation()
    {
        super("Inventory.parts.Rotation");
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        val packetAdapter = new InventoryRotationAdapter(this);
        return ModuleLoader.builder(this)
                           .addPacketListeners(packetAdapter)
                           .build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(100, 1).build();
    }

    private class InventoryRotationAdapter extends ModulePacketAdapter
    {
        public InventoryRotationAdapter(Module module)
        {
            super(module, ListenerPriority.LOWEST, PacketType.Play.Client.LOOK, PacketType.Play.Client.POSITION_LOOK);
        }

        @Override
        public void onPacketReceiving(PacketEvent packetEvent)
        {
            val user = User.safeGetUserFromPacketEvent(packetEvent);
            if (User.isUserInvalid(user, this.getModule())) return;

            final IWrapperPlayClientLook lookWrapper = packetEvent::getPacket;

            // Not flying (may trigger some fps)
            if (!user.getPlayer().getAllowFlight() &&
                // Player is in an inventory
                user.hasOpenInventory() &&
                // Head-Rotation has changed (detection)
                (user.getPlayer().getLocation().getYaw() != lookWrapper.getYaw() ||
                 user.getPlayer().getLocation().getPitch() != lookWrapper.getPitch()) &&
                // No recently tp
                !user.hasTeleportedRecently(teleportTime) &&
                !user.hasChangedWorldsRecently(worldChangeTime) &&
                // The player has opened his inventory for at least one second.
                user.notRecentlyOpenedInventory(1000))
            {
                getManagement().flag(Flag.of(user).setEventNotCancelledAction(() -> DebugSender.getInstance().sendDebug("Inventory-Debug | Player: " + user.getPlayer().getName() + " sent new rotations while having an open inventory.")));
            }
        }
    }
}
