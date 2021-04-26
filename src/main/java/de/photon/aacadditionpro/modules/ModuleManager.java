package de.photon.aacadditionpro.modules;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class ModuleManager
{
    private static final Map<String, Module> MODULES;

    static {
        final ImmutableMap.Builder<String, Module> builder = ImmutableMap.builder();



        MODULES = builder.build();
    }
}
