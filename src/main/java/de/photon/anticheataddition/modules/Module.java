package de.photon.anticheataddition.modules;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.InternalPermission;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Locale;
import java.util.Set;

@EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY, onlyExplicitlyIncluded = true)
@ToString
public abstract class Module implements ConfigLoading
{
    @Getter protected final String configString;
    @Getter @EqualsAndHashCode.Include private final String moduleId;
    @Getter private final String bypassPermission = (InternalPermission.BYPASS.getRealPermission() + '.') + this.getModuleId();
    @Getter(lazy = true) private final ModuleLoader moduleLoader = Preconditions.checkNotNull(createModuleLoader(), "Tried to create null ModuleLoader.");
    @Getter private boolean enabled = false;
    @Getter private final Set<Module> children;

    private final EventBus toChildren = new EventBus();
    private final EventBus toParent = new EventBus();

    private Module(String configString, Set<Module> children)
    {
        Preconditions.checkNotNull(configString, "Tried to create Module with null configString.");
        Preconditions.checkArgument(AntiCheatAddition.getInstance().getConfig().contains(configString), "Config path " + configString + " does not exist in the config. Please regenerate your config.");
        this.configString = configString;
        this.moduleId = "anticheataddition_" + configString.toLowerCase(Locale.ENGLISH);
        this.children = Set.copyOf(children);
    }

    protected Module(String configString)
    {
        this(configString, Set.of());
    }

    protected Module(String configString, Module... children)
    {
        this(configString, Set.of(children));
    }

    public void forwardToChildren(Object object)
    {
        this.toChildren.post(object);
    }

    public void forwardToParent(Object object)
    {
        this.toParent.post(object);
    }

    void setupEventBuses()
    {
        for (Module child : children) {
            if (child.moduleLoader.isAllowLoading()) {
                this.toChildren.register(child);
                child.toParent.register(this);
            }
        }
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
        if (!this.enabled && this.getModuleLoader().load()) {
            this.enabled = true;
            this.enable();
        }
    }

    public final void disableModule()
    {
        if (this.enabled) {
            this.enabled = false;
            this.getModuleLoader().unload();
            this.disable();
        }
    }

    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this).build();
    }

    protected void enable() {}

    protected void disable() {}
}
