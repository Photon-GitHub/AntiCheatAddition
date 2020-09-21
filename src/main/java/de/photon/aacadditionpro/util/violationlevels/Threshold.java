package de.photon.aacadditionpro.util.violationlevels;

import de.photon.aacadditionpro.util.files.configs.ConfigUtils;
import lombok.Getter;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

    /**
     * Tries to load all thresholds from the given config key.
     *
     * @param thresholdSectionPath the given path to the section that contains the thresholds
     *
     * @return a mutable {@link List} containing all {@link Threshold}s.
     */
    public static List<Threshold> loadThresholds(final String thresholdSectionPath)
    {
        // Make sure that the keys exist.
        return Objects.requireNonNull(ConfigUtils.loadKeys(thresholdSectionPath), "Severe loading error: Keys are null when loading: " + thresholdSectionPath)
                      .stream()
                      // Create a new Threshold for every key.
                      .map(key -> new Threshold(Integer.parseInt(key), ConfigUtils.loadStringOrStringList(thresholdSectionPath + '.' + key)))
                      // Collect the keys.
                      .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Threshold threshold = (Threshold) o;
        return vl == threshold.vl;
    }

    @Override
    public int hashCode()
    {
        return vl;
    }

    @Override
    public int compareTo(Threshold o)
    {
        return Integer.compare(vl, o.vl);
    }
}
