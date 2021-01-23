package de.photon.aacadditionproold.util.files.configs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to load values from the config.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface LoadFromConfiguration
{
    String configPath();

    Class listType() default Object.class;
}
