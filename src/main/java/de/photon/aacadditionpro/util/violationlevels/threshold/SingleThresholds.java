package de.photon.aacadditionpro.util.violationlevels.threshold;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

@RequiredArgsConstructor
class SingleThresholds implements ThresholdManagement
{
    private final Threshold threshold;

    @Override
    public void executeThresholds(int fromVl, int toVl, @NotNull Collection<Player> players)
    {
        if (fromVl < threshold.getVl() && toVl >= threshold.getVl()) {
            threshold.executeCommandList(players);
        }
    }
}
