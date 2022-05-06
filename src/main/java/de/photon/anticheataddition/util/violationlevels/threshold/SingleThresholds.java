package de.photon.anticheataddition.util.violationlevels.threshold;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
final class SingleThresholds implements ThresholdManagement
{
    private final Threshold threshold;

    @Override
    public void executeThresholds(int fromVl, int toVl, @NotNull Player player)
    {
        if (fromVl < threshold.getVl() && toVl >= threshold.getVl()) threshold.executeCommandList(player);
    }
}
