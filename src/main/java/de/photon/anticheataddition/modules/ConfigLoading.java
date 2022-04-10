package de.photon.anticheataddition.modules;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.util.config.ConfigUtil;

import java.util.List;

/**
 * Defines methods to load values from the config.
 */
public interface ConfigLoading
{
    /**
     * The base {@link String} the values are loaded from.
     */
    String getConfigString();

    default boolean loadBoolean(String substring, boolean def)
    {
        return AntiCheatAddition.getInstance().getConfig().getBoolean(getConfigString() + substring, def);
    }

    default int loadInt(String substring, int def)
    {
        return AntiCheatAddition.getInstance().getConfig().getInt(getConfigString() + substring, def);
    }

    default long loadLong(String substring, long def)
    {
        return AntiCheatAddition.getInstance().getConfig().getLong(getConfigString() + substring, def);
    }

    default double loadDouble(String substring, double def)
    {
        return AntiCheatAddition.getInstance().getConfig().getDouble(getConfigString() + substring, def);
    }

    default String loadString(String substring, String def)
    {
        return AntiCheatAddition.getInstance().getConfig().getString(getConfigString() + substring, def);
    }

    default List<String> loadStringList(String substring)
    {
        return ConfigUtil.loadImmutableStringOrStringList(getConfigString() + substring);
    }
}
