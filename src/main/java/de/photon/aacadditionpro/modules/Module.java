package de.photon.aacadditionpro.modules;

import de.photon.aacadditionpro.InternalPermission;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Locale;

@EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY, onlyExplicitlyIncluded = true)
@ToString
public abstract class Module
{
    @Getter protected final String configString;
    @Getter @EqualsAndHashCode.Include private final String moduleId;
    protected final String bypassPermission = InternalPermission.bypassPermissionOf(this.getModuleId());
    @Getter(lazy = true) private final ModuleLoader moduleLoader = createModuleLoader();
    @Getter private boolean enabled;

    public Module(String configString)
    {
        this.configString = configString;
        this.moduleId = generateModuleId(configString);
    }

    public static String generateModuleId(final String configString)
    {
        return "aacadditionpro_" + configString.toLowerCase(Locale.ENGLISH);
    }

    public void setEnabled(boolean enabled)
    {
        if (this.enabled != enabled) {
            if (enabled) {
                enableModule();
            } else {
                disableModule();
            }
        }
    }

    public void enableModule()
    {
        this.enabled = true;
        this.getModuleLoader().load();
        this.enable();
    }

    public void disableModule()
    {
        this.enabled = false;
        this.getModuleLoader().unload();
        this.disable();
    }

    protected abstract ModuleLoader createModuleLoader();

    protected void enable() {}

    protected void disable() {}
}
