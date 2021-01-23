package de.photon.aacadditionproold.modules.checks.keepalive;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.aacadditionproold.AACAdditionPro;
import de.photon.aacadditionproold.modules.Module;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.modules.PacketListenerModule;
import de.photon.aacadditionproold.modules.RestrictedBungeecord;
import de.photon.aacadditionproold.user.User;
import de.photon.aacadditionproold.user.UserManager;
import de.photon.aacadditionproold.util.messaging.VerboseSender;
import lombok.Getter;

/**
 * This {@link Module} flags KeepAlive packets that are ignored by the client.
 */
class KeepAliveIgnoredPattern extends PacketAdapter implements PacketListenerModule, RestrictedBungeecord
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
