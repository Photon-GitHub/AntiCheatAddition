package de.photon.anticheataddition.util.violationlevels.threshold;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class EmptyThresholds implements ThresholdManagement
{
    static final EmptyThresholds INSTANCE = new EmptyThresholds();

    @Override
    public void executeThresholds(int fromVl, int toVl, @NotNull Player players)
    {
        // Do nothing, as there are no thresholds.
    }
}
