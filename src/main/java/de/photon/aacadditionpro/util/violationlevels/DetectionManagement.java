package de.photon.aacadditionpro.util.violationlevels;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.events.SentinelEvent;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.data.Constants;
import de.photon.aacadditionpro.util.violationlevels.threshold.ThresholdManagement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DetectionManagement extends ViolationManagement
{
    private final Set<UUID> detectionSet = ConcurrentHashMap.newKeySet(Constants.SERVER_EXPECTED_PLAYERS);

    public DetectionManagement(ViolationModule module)
    {
        super(module, ThresholdManagement.loadCommands(module.getConfigString() + ".commands"));
    }

    @Override
    public void flag(@NotNull Flag flag)
    {
        Preconditions.checkArgument(flag.getAddedVl() == 1, "Tried to add more than 1 vl in detection management.");

        if (!SentinelEvent.build(flag.getPlayer(), this.module.getModuleId()).call().isCancelled()) {
            this.addVL(flag.getPlayer(), flag.getAddedVl());
            // No execution of the Runnables of flag.
        }
    }

    @Override
    public int getVL(@NotNull UUID uuid)
    {
        return detectionSet.contains(uuid) ? 1 : 0;
    }

    @Override
    public void setVL(@NotNull Player player, int newVl)
    {
        Preconditions.checkArgument(newVl <= 1, "A Sentinel detection management only supports the vls 0 (no detection) and 1 (detection).");

        if (newVl == 0) {
            this.detectionSet.remove(player.getUniqueId());
        } else {
            this.addVL(player, 1);
        }
    }

    @Override
    protected void addVL(@NotNull Player player, int vl)
    {
        Preconditions.checkArgument(vl <= 1, "A Sentinel detection management only supports the vls 0 (no detection) and 1 (detection).");

        // Only punish if the detection is new.
        if (detectionSet.add(player.getUniqueId())) {
            this.punishPlayer(player, 0, 1);
        }
    }
}
