package de.photon.AACAdditionPro.util.files;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to load values from the config.
 * If this is used in an instance of {@link de.photon.AACAdditionPro.checks.AACAdditionProCheck} it will automatically
 * put the {@link de.photon.AACAdditionPro.AdditionHackType} - configstring in front of configPath, otherwise you need
 * to write down the full path.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface LoadFromConfiguration
{
    String configPath();
}
