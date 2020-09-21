package de.photon.aacadditionpro.modules.checks.keepalive;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PacketListenerModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.util.messaging.VerboseSender;
import lombok.Getter;

/**
 * This {@link de.photon.aacadditionpro.modules.Module} flags KeepAlive packets that are ignored by the client.
 */
class KeepAliveIgnoredPattern extends PacketAdapter implements PacketListenerModule
{
    @Getter
    private static final KeepAliveIgnoredPattern instance = new KeepAliveIgnoredPattern();

    public KeepAliveIgnoredPattern()
    {
        super(AACAdditionPro.getInstance(), ListenerPriority.LOW, PacketType.Play.Server.KEEP_ALIVE);
    }

    @Override
    public void onPacketSending(PacketEvent event)
    {
        final User user = UserManager.safeGetUserFromPacketEvent(event);

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        final int ignored = user.getKeepAliveData().getIgnoredKeepAlives().getAndSet(0);
        KeepAlive.getInstance().getViolationLevelManagement().flag(user.getPlayer(), ignored * 10,
                                                                   -1, () -> {},
                                                                   () -> VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " ignored " + ignored + "KeepAlive packets"));
    }

    @Override
    public boolean isSubModule()
    {
        return true;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.ignored";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.KEEPALIVE;
    }
}
