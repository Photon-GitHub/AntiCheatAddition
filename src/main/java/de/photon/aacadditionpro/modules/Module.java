package de.photon.aacadditionpro.modules;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.AACAdditionPro;
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
    @Getter(lazy = true) private final ModuleLoader moduleLoader = Preconditions.checkNotNull(createModuleLoader(), "Tried to create null ModuleLoader.");
    @Getter private boolean enabled;

    public Module(String configString)
    {
        Preconditions.checkNotNull(configString, "Tried to create Module with null configString.");
        Preconditions.checkArgument(AACAdditionPro.getInstance().getConfig().contains(configString), "Config path " + configString + " does not exist in the config. Please regenerate your config.");
        this.configString = configString;
        this.moduleId = "aacadditionpro_" + configString.toLowerCase(Locale.ENGLISH);
    }

    public void setEnabled(boolean enabled)
    {
        if (this.enabled != enabled) {
            if (enabled) enableModule();
            else disableModule();
        }
    }

    public final void enableModule()
    {
        this.enabled = true;
        this.getModuleLoader().load();
        this.enable();
    }

    public final void disableModule()
    {
        this.enabled = false;
        this.getModuleLoader().unload();
        this.disable();
    }

    protected abstract ModuleLoader createModuleLoader();

    protected void enable() {}

    protected void disable() {}
}
