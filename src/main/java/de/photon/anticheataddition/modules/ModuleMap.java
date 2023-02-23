package de.photon.anticheataddition.modules;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ModuleMap<T extends Module>
{
    private Map<String, T> backingMap;

    public ModuleMap(Collection<T> modules)
    {
        // Collect an unmodifiable map from moduleId to module.
        this.backingMap = modules.stream().collect(Collectors.toUnmodifiableMap(Module::getModuleId, Function.identity()));
    }

    @Nullable
    public T getModule(String moduleId)
    {
        return this.backingMap.get(moduleId);
    }

    public synchronized void addModule(T module)
    {
        this.backingMap = Stream.concat(this.values().stream(), Stream.of(module))
                                .collect(Collectors.toUnmodifiableMap(Module::getModuleId, Function.identity()));
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
