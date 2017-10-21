package de.photon.AACAdditionPro.util.files;

import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ViolationModule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to load values from the config.
 * If this is used in an instance of {@link ViolationModule} it will automatically
 * put the {@link ModuleType} - configstring in front of configPath, otherwise you need
 * to write down the full path.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface LoadFromConfiguration
{
    String configPath();

    Class listType() default Object.class;
}
