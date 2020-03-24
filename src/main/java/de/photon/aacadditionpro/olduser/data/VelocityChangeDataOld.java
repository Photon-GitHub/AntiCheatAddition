package de.photon.aacadditionpro.olduser.data;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.olduser.TimeDataOld;
import de.photon.aacadditionpro.olduser.UserManager;
import de.photon.aacadditionpro.olduser.UserOld;
import de.photon.aacadditionpro.util.packetwrappers.IWrapperPlayPosition;
import org.bukkit.event.Listener;

public class VelocityChangeDataOld extends TimeDataOld
{
    static {
        VelocityChangeDataUpdater velocityChangeDataUpdater = new VelocityChangeDataUpdater();
        ProtocolLibrary.getProtocolManager().addPacketListener(velocityChangeDataUpdater);
    }

    public boolean positiveVelocity;

    public VelocityChangeDataOld(final UserOld user)
    {
        // [0] last velocity change
        super(user, 0);

    }

    /**
     * A singleton class to reduce the reqired {@link Listener}s to a minimum.
     */
    private static class VelocityChangeDataUpdater extends PacketAdapter
    {
        private VelocityChangeDataUpdater()
        {
            super(AACAdditionPro.getInstance(), ListenerPriority.MONITOR, PacketType.Play.Client.POSITION, PacketType.Play.Client.POSITION_LOOK);
        }

        @Override
        public void onPacketReceiving(final PacketEvent event)
        {
            if (event.getPlayer() == null || event.isPlayerTemporary()) {
                return;
            }

            final UserOld user = UserManager.getUser(event.getPlayer().getUniqueId());

            // The player wasn't hurt and got velocity for that.
            if (user != null
                && user.getPlayer().getNoDamageTicks() == 0
                // Recent teleports can cause bugs
                && !user.getTeleportData().recentlyUpdated(0, 1000))
            {
                final IWrapperPlayPosition position = event::getPacket;

                final boolean updatedPositiveVelocity = user.getPlayer().getLocation().getY() < position.getY();

                if (updatedPositiveVelocity != user.getVelocityChangeData().positiveVelocity) {
                    user.getVelocityChangeData().positiveVelocity = updatedPositiveVelocity;
                    user.getVelocityChangeData().updateTimeStamp(0);
                }
            }
        }
    }
}
