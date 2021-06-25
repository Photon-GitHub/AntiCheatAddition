package de.photon.aacadditionpro.modules.checks.fastswitch;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ModulePacketAdapter;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.inventory.InventoryUtil;
import de.photon.aacadditionpro.util.mathematics.MathUtil;
import de.photon.aacadditionpro.util.server.PingProvider;
import de.photon.aacadditionpro.util.server.TPSProvider;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import lombok.val;

public class Fastswitch extends ViolationModule
{
    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancelVl;

    @LoadFromConfiguration(configPath = "max_ping")
    private double maxPing;

    @LoadFromConfiguration(configPath = "switch_milliseconds")
    private int switchMilliseconds;

    public Fastswitch()
    {
        super("Fastswitch");
    }

    /**
     * Used to acknowledge if somebody can be legit.
     * I.e. that players can scroll very fast, but then the neighbor slot is always the one that gets called next.
     */
    private static boolean canBeLegit(final int oldSlot, final int newHeldItemSlot)
    {
        return (oldSlot == 0 && newHeldItemSlot == 8) ||
               (oldSlot == 8 && newHeldItemSlot == 0) ||
               MathUtil.roughlyEquals(oldSlot, newHeldItemSlot, 1);
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        val adapter = new FastswitchPacketAdapter(this);
        return ModuleLoader.builder(this)
                           .addPacketListeners(adapter)
                           .build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).withDecay(120, 25).build();
    }

    private class FastswitchPacketAdapter extends ModulePacketAdapter
    {
        public FastswitchPacketAdapter(Module module)
        {
            super(module, ListenerPriority.NORMAL, PacketType.Play.Client.HELD_ITEM_SLOT);
        }

        @Override
        public void onPacketReceiving(final PacketEvent event)
        {
            val user = User.safeGetUserFromPacketEvent(event);
            if (User.isUserInvalid(user, this.getModule())) return;

            // Tps are high enough
            if (TPSProvider.getTPS() > 19 &&
                event.getPacket().getBytes().readSafely(0) != null &&
                // Prevent the detection of scrolling
                !canBeLegit(user.getPlayer().getInventory().getHeldItemSlot(), event.getPacket().getBytes().readSafely(0)))
            {
                // Already switched in the given timeframe
                if (user.getTimestampMap().at(TimestampKey.LAST_HOTBAR_SWITCH).recentlyUpdated(switchMilliseconds)
                    // The ping is valid and in the borders that are set in the config
                    && (maxPing < 0 || PingProvider.getPing(user.getPlayer()) < maxPing))
                {
                    getManagement().flag(Flag.of(user)
                                             .setAddedVl(25)
                                             .setCancelAction(cancelVl, () -> event.setCancelled(true))
                                             .setEventNotCancelledAction(() -> InventoryUtil.syncUpdateInventory(user.getPlayer())));
                }

                user.getTimestampMap().at(TimestampKey.LAST_HOTBAR_SWITCH).update();
            }
        }
    }
}
