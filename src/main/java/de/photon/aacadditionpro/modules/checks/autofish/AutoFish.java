package de.photon.aacadditionpro.modules.checks.autofish;

import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.modules.ListenerModule;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.modules.RestrictedServerVersion;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.Set;

public class AutoFish implements ListenerModule, PatternModule, ViolationModule, RestrictedServerVersion
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 600);

    private final Pattern<User, PlayerFishEvent> consistencyPattern = new ConsistencyPattern();
    private final Pattern<User, PlayerFishEvent> inhumanReactionPattern = new InhumanReactionPattern();

    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancelVl;

    @EventHandler
    public void on(final PlayerFishEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // User valid and not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        vlManager.flag(event.getPlayer(),
                       this.consistencyPattern.apply(user, event) + this.inhumanReactionPattern.apply(user, event),
                       cancelVl,
                       () -> event.setCancelled(true), () -> {});
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
