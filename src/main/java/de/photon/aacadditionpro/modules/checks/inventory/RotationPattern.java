package de.photon.aacadditionpro.modules.checks.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.packetwrappers.client.IWrapperPlayClientLook;

class RotationPattern extends PatternModule.PacketPattern
{
    @LoadFromConfiguration(configPath = ".teleport_time")
    private int teleportTime;

    protected RotationPattern()
    {
        super(ImmutableSet.of(PacketType.Play.Client.LOOK, PacketType.Play.Client.POSITION_LOOK));
    }

    @Override
    protected int process(User user, PacketEvent packetEvent)
    {
        final IWrapperPlayClientLook lookWrapper = packetEvent::getPacket;

        // Not flying (may trigger some fps)
        if (!user.getPlayer().getAllowFlight() &&
            // Player is in an inventory
            user.hasOpenInventory() &&
            // Head-Rotation has changed (detection)
            (user.getPlayer().getLocation().getYaw() != lookWrapper.getYaw() ||
             user.getPlayer().getLocation().getPitch() != lookWrapper.getPitch()) &&
            // No recently tp
            !user.hasTeleportedRecently(this.teleportTime) &&
            // The player has opened his inventory for at least one second.
            user.notRecentlyOpenedInventory(1000))
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
