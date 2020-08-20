package de.photon.aacadditionpro.modules.checks.keepalive;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PacketListenerModule;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.user.subdata.KeepAliveData;
import de.photon.aacadditionpro.util.messaging.VerboseSender;
import de.photon.aacadditionpro.util.packetwrappers.WrapperPlayKeepAlive;
import de.photon.aacadditionpro.util.packetwrappers.client.WrapperPlayClientKeepAlive;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import lombok.Getter;

import java.util.Iterator;
import java.util.Set;

public class KeepAlive extends PacketAdapter implements PacketListenerModule, ViolationModule
{
    @Getter
    private static final KeepAlive instance = new KeepAlive();
    private static final Set<Module> submodules = ImmutableSet.of(KeepAliveIgnoredPattern.getInstance(),
                                                                  KeepAliveInjectPattern.getInstance(),
                                                                  KeepAliveOffsetPattern.getInstance());

    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 200);

    public KeepAlive()
    {
        super(AACAdditionPro.getInstance(), ListenerPriority.LOW, PacketType.Play.Client.KEEP_ALIVE);
    }

    @Override
    public void onPacketReceiving(final PacketEvent event)
    {
        final User user = UserManager.safeGetUserFromPacketEvent(event);

        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        final WrapperPlayKeepAlive wrapper = new WrapperPlayClientKeepAlive(event.getPacket());

        final long keepAliveId = wrapper.getKeepAliveId();
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
        } else {
            keepAlivePacketData.registerResponse();
            KeepAliveOffsetPattern.getInstance().getApplyingConsumer().accept(user, offset);
        }
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public Set<Module> getSubModules()
    {
        return submodules;
    }

    @Override
    public boolean isSubModule()
    {
        return false;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.KEEPALIVE;
    }
}
