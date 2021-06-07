package de.photon.aacadditionpro.modules.checks.skinblinker;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ModulePacketAdapter;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import lombok.val;

public class SkinBlinker extends ViolationModule
{
    public SkinBlinker()
    {
        super("Skinblinker");
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        val adapter = new SkinblinkerPacketAdapter(this);
        return ModuleLoader.builder(this)
                           .addPacketListeners(adapter)
                           .build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return new ViolationLevelManagement(this, 100L, 1);
    }

    private class SkinblinkerPacketAdapter extends ModulePacketAdapter
    {
        public SkinblinkerPacketAdapter(Module module)
        {
            super(module, ListenerPriority.NORMAL, PacketType.Play.Client.SETTINGS);
        }

        @Override
        public void onPacketReceiving(final PacketEvent event)
        {
            /*
             * A unmodified client can only send such packets if the player is in the menu
             * -> he obviously cannot Sprint or Sneak when doing this.
             * -> he can move, especially in MC 1.9 and upward because of entity-collision, etc.
             * -> As of the render-debug-cycle which can be done in the game (F3 + F) I need to check for the change of the skin.
             */
            val user = User.safeGetUserFromPacketEvent(event);
            if (User.isUserInvalid(user, this.getModule())) return;

            // Sprinting or sneaking (detection)
            if ((event.getPlayer().isSprinting() || event.getPlayer().isSneaking())) {
                val newSkinComponents = event.getPacket().getIntegers().readSafely(1);

                // updateSkinComponents returns true if the skin has changed.
                if (user.updateSkinComponents(newSkinComponents)) getManagement().flag(Flag.of(user));
            }
        }
    }
}