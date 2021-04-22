package de.photon.aacadditionpro.util.violationlevels.threshold;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import java.util.Collection;

@RequiredArgsConstructor
class SingleThresholds extends ThresholdManagement
{
    private final Threshold threshold;

    @Override
    public void executeThresholds(int fromVl, int toVl, Collection<Player> players)
    {
        if (fromVl < threshold.getVl() && toVl >= threshold.getVl()) {
            threshold.executeCommandList(players);
        }
    }
}
