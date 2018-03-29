package de.photon.AACAdditionPro.util.files.configs;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import java.lang.reflect.Field;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Getter(AccessLevel.PACKAGE)
final class ConfigField
{

    @NonNull
    private final Field reflectionField;
    @NonNull
    private final String customPath;
    private final String comment;

    String getFieldName()
    {
        return getReflectionField().getName();
    }

    boolean hasComment()
    {
        return comment != null && !comment.isEmpty();
    }

    boolean hasCustomPath()
    {
        return customPath != null && !customPath.isEmpty();
    }

    String getResultingPath()
    {
        return hasCustomPath() ? getCustomPath() : getFieldName().replace('_', '.');
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConfigField that = (ConfigField) o;

        if (!comment.equals(that.comment)) return false;
        if (!customPath.equals(that.customPath)) return false;
        if (!reflectionField.equals(that.reflectionField)) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = reflectionField.hashCode();
        result = 31 * result + customPath.hashCode();
        result = 31 * result + comment.hashCode();
        return result;
    }
}