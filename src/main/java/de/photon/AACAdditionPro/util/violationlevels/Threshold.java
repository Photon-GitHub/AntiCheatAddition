package de.photon.AACAdditionPro.util.violationlevels;

import de.photon.AACAdditionPro.util.files.configs.ConfigUtils;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

class Threshold implements Comparable<Threshold>
{
    @Getter
    private final int vl;
    @Getter
    private final List<String> commandList;

    public Threshold(int vl, List<String> commandList)
    {
        this.vl = vl;
        this.commandList = commandList;
    }

    @Override
    public int compareTo(Threshold o)
    {
        return Integer.compare(vl, o.vl);
    }

    /**
     * Tries to load all thresholds from the given config key.
     *
     * @param thresholdSectionPath the given path to the section that contains the thresholds
     *
     * @return a mutable {@link List} containing all {@link Threshold}s.
     */
    public static List<Threshold> loadThresholds(final String thresholdSectionPath)
    {
        final Set<String> keys = Objects.requireNonNull(ConfigUtils.loadKeys(thresholdSectionPath), "Severe loading error: Keys are null when loading: " + thresholdSectionPath);

        final List<Threshold> builder = new ArrayList<>();

        for (final String key : keys)
        {
            //Put the command into thresholds
            builder.add(new Threshold(Integer.parseInt(key), ConfigUtils.loadStringOrStringList(thresholdSectionPath + "." + key)));
        }

        return builder;
    }
}
