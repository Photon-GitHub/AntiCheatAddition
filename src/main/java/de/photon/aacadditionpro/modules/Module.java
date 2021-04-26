package de.photon.aacadditionpro.modules;

import de.photon.aacadditionpro.InternalPermission;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Locale;

@EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY, onlyExplicitlyIncluded = true)
@ToString
public abstract class Module
{
    @Getter protected final String configString;
    @Getter @EqualsAndHashCode.Include private final String moduleId;
    protected final String bypassPermission = InternalPermission.bypassPermissionOf(this.getModuleId());
    @Getter(lazy = true) private final ModuleLoader moduleLoader = createModuleLoader();
    @Getter private boolean loaded;

    public Module(String configString)
    {
        this.configString = configString;
        this.moduleId = "aacadditionpro_" + configString.toLowerCase(Locale.ENGLISH);
    }

    public static ViolationManagement.Flag createFlag(Player player)
    {
        return ViolationManagement.createFlag(player);
    }

    public static ViolationManagement.Flag createFlag(Collection<Player> players)
    {
        return ViolationManagement.createFlag(players);
    }

    public void setEnabled(boolean enabled)
    {
        if (loaded != enabled) {
            if (enabled) {
                enableModule();
            } else {
                disableModule();
            }
        }
    }

    public void enableModule()
    {
        this.loaded = true;
        this.moduleLoader.load();
        this.enable();
    }

    public void disableModule()
    {
        this.loaded = false;
        this.moduleLoader.unload();
        this.disable();
    }

    protected abstract ModuleLoader createModuleLoader();

    protected void enable() {}

    protected void disable() {}
}
