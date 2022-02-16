package de.photon.aacadditionpro.modules;

import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ModuleMap<T extends Module>
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

    public void addModule(T module)
    {
        val copyMap = new HashMap<>(backingMap);
        copyMap.put(module.getModuleId(), module);
        backingMap = Map.copyOf(copyMap);
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
