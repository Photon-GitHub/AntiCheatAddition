package de.photon.AACAdditionPro.modules.checks.keepalive;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PacketListenerModule;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.modules.ViolationModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.user.data.KeepAliveData;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.packetwrappers.client.WrapperPlayClientKeepAlive;
import de.photon.AACAdditionPro.util.packetwrappers.server.WrapperPlayServerKeepAlive;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;

import java.util.Iterator;
import java.util.Set;

public class KeepAlive extends PacketAdapter implements PacketListenerModule, PatternModule, ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 200);

    private final KeepAliveIgnoredPattern keepAliveIgnoredPattern = new KeepAliveIgnoredPattern();
    private final KeepAliveInjectPattern keepAliveInjectPattern = new KeepAliveInjectPattern();
    private final KeepAliveOffsetPattern keepAliveOffsetPattern = new KeepAliveOffsetPattern();

    public KeepAlive()
    {
        super(AACAdditionPro.getInstance(), ListenerPriority.LOW,
              // --------------- Server --------------- //
              // KeepAlive analysis
              PacketType.Play.Server.KEEP_ALIVE,
              // --------------- Client --------------- //
              // KeepAlive analysis
              PacketType.Play.Client.KEEP_ALIVE);
    }

    @Override
    public void onPacketSending(PacketEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        // Register the KeepAlive
        synchronized (user.getKeepAliveData().getKeepAlives()) {
            user.getKeepAliveData().getKeepAlives().add(new KeepAliveData.KeepAlivePacketData(new WrapperPlayServerKeepAlive(event.getPacket()).getKeepAliveId()));
        }
        vlManager.flag(user.getPlayer(), keepAliveIgnoredPattern.apply(user, event), -1, () -> {}, () -> {});
    }

    @Override
    public void onPacketReceiving(final PacketEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        final long keepAliveId = new WrapperPlayClientKeepAlive(event.getPacket()).getKeepAliveId();
        KeepAliveData.KeepAlivePacketData keepAlivePacketData = null;

        int offset = 0;
        synchronized (user.getKeepAliveData().getKeepAlives()) {
            final Iterator<KeepAliveData.KeepAlivePacketData> iterator = user.getKeepAliveData().getKeepAlives().descendingIterator();
            KeepAliveData.KeepAlivePacketData current;
            while (iterator.hasNext()) {
                current = iterator.next();

                if (current.getKeepAliveID() == keepAliveId) {
                    keepAlivePacketData = current;
                    break;
                }

                offset++;
            }
        }

        // A packet with the same data must have been sent before.
        if (keepAlivePacketData == null ||
            // If the packet already has a response something is off.
            keepAlivePacketData.hasRegisteredResponse())
        {
            VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " sent unregistered KeepAlive packet.");
            vlManager.flag(user.getPlayer(), 20, -1, () -> {}, () -> {});
        }
        else {
            keepAlivePacketData.registerResponse();
            vlManager.flag(user.getPlayer(), keepAliveOffsetPattern.apply(user, offset), -1, () -> {}, () -> {});
        }
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public Set<PatternModule.Pattern> getPatterns()
    {
        return ImmutableSet.of(keepAliveIgnoredPattern,
                               keepAliveInjectPattern,
                               keepAliveOffsetPattern);
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.KEEPALIVE;
    }
}
