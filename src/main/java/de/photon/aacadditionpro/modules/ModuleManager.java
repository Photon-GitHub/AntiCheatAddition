package de.photon.aacadditionpro.modules;

import com.google.common.collect.ImmutableMap;
import de.photon.aacadditionpro.modules.checks.AutoEat;
import lombok.Getter;

import java.util.Map;

public class ModuleManager
{
    @Getter
    private static final Map<String, Module> MODULES;

    static {
        final ImmutableMap.Builder<String, Module> builder = ImmutableMap.builder();
        addModule(builder, new AutoEat());


        MODULES = builder.build();
    }

    private static void addModule(ImmutableMap.Builder<String, Module> builder, Module module)
    {
        builder.put(module.getModuleId(), module);
    }
}
