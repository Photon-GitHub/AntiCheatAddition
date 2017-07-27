package de.photon.AACAdditionPro.userdata;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.AACAdditionPro.AACAdditionPro;

class BeaconListener extends PacketAdapter
{
    BeaconListener()
    {
        super(AACAdditionPro.getInstance(), ListenerPriority.MONITOR, PacketType.Play.Client.CUSTOM_PAYLOAD);
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }

    @Override
    public void onPacketReceiving(final PacketEvent event)
    {
        if (!event.isCancelled() &&
            event.getPacket().getStrings().readSafely(0).equalsIgnoreCase("MC|Beacon"))
        {
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());
            if (user != null) {
                // User has made a beacon action/transaction so the inventory must internally be closed this way as no
                // InventoryCloseEvent is fired.
                user.getInventoryData().nullifyTimeStamp();
            }
        }
    }
}
