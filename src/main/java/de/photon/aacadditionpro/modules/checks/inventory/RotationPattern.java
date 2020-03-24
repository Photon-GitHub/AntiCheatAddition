package de.photon.aacadditionpro.modules.checks.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.olduser.UserOld;
import de.photon.aacadditionpro.util.packetwrappers.client.IWrapperPlayClientLook;

class RotationPattern extends PatternModule.PacketPattern
{
    protected RotationPattern()
    {
        super(ImmutableSet.of(PacketType.Play.Client.LOOK, PacketType.Play.Client.POSITION_LOOK));
    }

    @Override
    protected int process(UserOld user, PacketEvent packetEvent)
    {
        final IWrapperPlayClientLook lookWrapper = packetEvent::getPacket;

        // Not flying (may trigger some fps)
        if (!user.getPlayer().getAllowFlight() &&
            // Player is in an inventory
            user.getInventoryData().hasOpenInventory() &&
            // Head-Rotation has changed (detection)
            (user.getPlayer().getLocation().getYaw() != lookWrapper.getYaw() ||
             user.getPlayer().getLocation().getPitch() != lookWrapper.getPitch()) &&
            // No recently tp
            !user.getTeleportData().recentlyUpdated(0, 1000) &&
            // The player has opened his inventory for at least one second.
            user.getInventoryData().notRecentlyOpened(1000))
        {
            message = "Inventory-Verbose | Player: " + user.getPlayer().getName() + " sent new rotations while having an open inventory.";
            return 1;
        }
        return 0;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.Rotation";
    }


    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.INVENTORY;
    }
}
