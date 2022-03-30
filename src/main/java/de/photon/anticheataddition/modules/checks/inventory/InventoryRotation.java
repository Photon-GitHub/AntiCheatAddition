package de.photon.anticheataddition.modules.checks.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.protocol.packetwrappers.sentbyclient.IWrapperPlayClientLook;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import lombok.val;

public final class InventoryRotation extends ViolationModule
{
    public static final InventoryRotation INSTANCE = new InventoryRotation();

    private final int teleportTime = loadInt(".teleport_bypass_time", 900);
    private final int worldChangeTime = loadInt(".world_change_bypass_time", 2000);

    private InventoryRotation()
    {
        super("Inventory.parts.Rotation");
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .addPacketListeners(PacketAdapterBuilder.of(PacketType.Play.Client.LOOK, PacketType.Play.Client.POSITION_LOOK).priority(ListenerPriority.LOWEST).onReceiving(event -> {
                               val user = User.safeGetUserFromPacketEvent(event);
                               if (User.isUserInvalid(user, this)) return;

                               final IWrapperPlayClientLook lookWrapper = event::getPacket;

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
                                   getManagement().flag(Flag.of(user).setDebug("Inventory-Debug | Player: " + user.getPlayer().getName() + " sent new rotations while having an open inventory."));
                               }
                           }).build())
                           .build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(100, 1).build();
    }
}
