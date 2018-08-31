package de.photon.AACAdditionPro.modules.checks.autofish;

import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.ServerVersion;
import de.photon.AACAdditionPro.modules.ListenerModule;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.modules.RestrictedServerVersion;
import de.photon.AACAdditionPro.modules.ViolationModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.Set;

public class AutoFish implements ListenerModule, PatternModule, ViolationModule, RestrictedServerVersion
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 600);

    private final Pattern<User, PlayerFishEvent> consistencyPattern = new ConsistencyPattern();
    private final Pattern<User, PlayerFishEvent> inhumanReactionPattern = new InhumanReactionPattern();

    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancel_vl;

    @EventHandler
    public void on(final PlayerFishEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // User valid and not bypassed
        if (User.isUserInvalid(user, this.getModuleType()))
        {
            return;
        }

        int vl = this.consistencyPattern.apply(user, event);
        vl += this.inhumanReactionPattern.apply(user, event);

        vlManager.flag(event.getPlayer(), vl, cancel_vl, () -> event.setCancelled(true), () -> {});
    }

    @Override
    public Set<Pattern> getPatterns()
    {
        return ImmutableSet.of(consistencyPattern,
                               inhumanReactionPattern);
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.AUTO_FISH;
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public Set<ServerVersion> getSupportedVersions()
    {
        return ServerVersion.NON_188_VERSIONS;
    }
}
