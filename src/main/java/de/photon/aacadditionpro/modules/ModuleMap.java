package de.photon.aacadditionpro.modules;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class ModuleMap<T extends Module>
{
    private Map<String, T> backingMap;

    public ModuleMap(T... modules)
    {
        final ImmutableMap.Builder<String, T> builder = ImmutableMap.builder();
        for (T module : modules) builder.put(module.getModuleId(), module);
        this.backingMap = builder.build();
    }

    public ModuleMap(Iterable<T> modules)
    {
        final ImmutableMap.Builder<String, T> builder = ImmutableMap.builder();
        for (T module : modules) builder.put(module.getModuleId(), module);
        this.backingMap = builder.build();
    }

    @Nullable
    public T getModule(String moduleId)
    {
        return this.backingMap.get(moduleId);
    }

    public void addModule(T module)
    {
        final ImmutableMap.Builder<String, T> builder = ImmutableMap.builder();
        builder.putAll(backingMap);
        builder.put(module.getModuleId(), module);
        this.backingMap = builder.build();
    }

    public int size()
    {
        return backingMap.size();
    }

    public Set<String> keySet()
    {
        return backingMap.keySet();
    }

    public Collection<T> values()
    {
        return backingMap.values();
    }

    public Set<Map.Entry<String, T>> entrySet()
    {
        return backingMap.entrySet();
    }
}
