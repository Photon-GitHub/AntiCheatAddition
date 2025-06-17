package de.photon.anticheataddition.modules;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedSet;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.InternalPermission;
import de.photon.anticheataddition.util.log.Log;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;

@EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY, onlyExplicitlyIncluded = true)
@ToString
public class Module implements ConfigLoading, Comparable<Module>
{
    @Getter protected final String configString;
    @Getter @EqualsAndHashCode.Include private final String moduleId;
    @Getter private final String bypassPermission;
    @Getter(lazy = true) private final ModuleLoader moduleLoader = Preconditions.checkNotNull(createModuleLoader(), "Tried to create null ModuleLoader.");
    @Getter private boolean enabled = false;
    @Getter private final SortedSet<Module> children;

    private Module(String configString, Set<Module> children)
    {
        Preconditions.checkNotNull(configString, "Tried to create Module with null configString.");
        if (!AntiCheatAddition.getInstance().getConfig().contains(configString)) {
            final var message = "Config path " + configString + " does not exist in the config. Please regenerate your config. Further information can be found in the error below.";
            Log.severe(() -> message);
            throw new IllegalArgumentException(message);
        }
        this.configString = configString;
        this.moduleId = configString.toLowerCase(Locale.ENGLISH);
        this.bypassPermission = (InternalPermission.BYPASS.getRealPermission() + '.') + this.moduleId;
        this.children = ImmutableSortedSet.copyOf(children);
    }

    protected Module(String configString)
    {
        this(configString, Set.of());
    }

    protected Module(String configString, Module... children)
    {
        this(configString, Set.of(children));
    }

    public void activate()
    {
        if (this.enabled) return;

        if (this.getModuleLoader().load()) {
            for (Module child : children) child.activate();
            this.enabled = true;
            this.enable();
        }
    }

    public void deactivate()
    {
        this.getModuleLoader().unload();
        for (Module child : children) child.deactivate();
        this.enabled = false;
        this.disable();
    }

    @Override
    public int compareTo(@NotNull Module o)
    {
        return this.configString.compareTo(o.configString);
    }

    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this).build();
    }

    protected void enable() {}

    protected void disable() {}
}
