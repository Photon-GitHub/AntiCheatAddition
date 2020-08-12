package de.photon.aacadditionpro.modules.checks.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PacketListenerModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.messaging.VerboseSender;
import de.photon.aacadditionpro.util.packetwrappers.client.IWrapperPlayClientLook;
import lombok.Getter;

class RotationPattern extends PacketAdapter implements PacketListenerModule
{
    @Getter
    private static final RotationPattern instance = new RotationPattern();

    @LoadFromConfiguration(configPath = ".teleport_time")
    private int teleportTime;
    @LoadFromConfiguration(configPath = ".world_change_time")
    private int worldChangeTime;

    protected RotationPattern()
    {
        super(AACAdditionPro.getInstance(), ListenerPriority.LOWEST, PacketType.Play.Client.LOOK, PacketType.Play.Client.POSITION_LOOK);
    }


    @Override
    public void onPacketReceiving(PacketEvent packetEvent)
    {
        final User user = PacketListenerModule.safeGetUserFromEvent(packetEvent);

        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

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
            !user.hasChangedWorldsRecently(this.worldChangeTime) &&
            // The player has opened his inventory for at least one second.
            user.notRecentlyOpenedInventory(1000))
        {
            Inventory.getInstance().getViolationLevelManagement().flag(user.getPlayer(), 1, -1, () -> {},
                                                                       () -> VerboseSender.getInstance().sendVerboseMessage("Inventory-Verbose | Player: " + user.getPlayer().getName() + " sent new rotations while having an open inventory."));
        }
    }

    @Override
    public boolean isSubModule()
    {
        return true;
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
