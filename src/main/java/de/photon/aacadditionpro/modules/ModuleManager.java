package de.photon.aacadditionpro.modules;

import com.google.common.collect.ImmutableMap;
import de.photon.aacadditionpro.modules.checks.AutoEat;
import lombok.Getter;

import java.util.Map;

public class ModuleManager
{
    @Getter private static Map<String, Module> moduleMap;
    @Getter private static Map<String, Module> violationModuleMap;

    static {
        final ImmutableMap.Builder<String, Module> builder = ImmutableMap.builder();
        final ImmutableMap.Builder<String, Module> violationBuilder = ImmutableMap.builder();
        addModule(builder, violationBuilder, new AutoEat());

        moduleMap = builder.build();
        violationModuleMap = violationBuilder.build();
    }

    private static void addExternalModule(final Module externalModule)
    {
        moduleMap = withModule(moduleMap, externalModule);
        if (externalModule instanceof ViolationModule) violationModuleMap = withModule(violationModuleMap, externalModule);
    }

    private static void addModule(ImmutableMap.Builder<String, Module> moduleBuilder, ImmutableMap.Builder<String, Module> violationModuleBuilder, Module module)
    {
        moduleBuilder.put(module.getModuleId(), module);
        if (module instanceof ViolationModule) violationModuleBuilder.put(module.getModuleId(), module);
        module.enableModule();
    }

    private static Map<String, Module> withModule(Map<String, Module> map, Module module)
    {
        final ImmutableMap.Builder<String, Module> builder = ImmutableMap.builder();
        builder.putAll(map);
        builder.put(module.getModuleId(), module);
        return builder.build();
    }
}
