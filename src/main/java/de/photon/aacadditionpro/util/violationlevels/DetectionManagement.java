package de.photon.aacadditionpro.util.violationlevels;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.events.SentinelEvent;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DetectionManagement extends ViolationManagement
{
    private final Set<UUID> detectionSet = ConcurrentHashMap.newKeySet();

    /**
     * Create a new {@link ViolationManagement}
     *
     * @param moduleId   the module id of the module this {@link ViolationManagement} is being used by.
     * @param management the backing {@link ThresholdManagement}.
     */
    public DetectionManagement(String moduleId, ThresholdManagement management)
    {
        super(moduleId, management);
    }


    @Override
    public void flag(Flag flag)
    {
        Preconditions.checkArgument(flag.addedVl == 1, "Tried to add more than 1 vl in detection management.");

        if (!SentinelEvent.build(flag.player, this.moduleId).call().isCancelled()) {
            this.addVL(flag.player, flag.addedVl);
            // No execution of the Runnables of flag.
        }
    }

    @Override
    public int getVL(UUID uuid)
    {
        return detectionSet.contains(uuid) ? 1 : 0;
    }

    @Override
    public void setVL(Player player, int newVl)
    {
        Preconditions.checkArgument(newVl <= 1, "A Sentinel detection management only supports the vls 0 (no detection) and 1 (detection).");
        detectionSet.add(player.getUniqueId());
    }

    @Override
    protected void addVL(Player player, int vl)
    {
        Preconditions.checkArgument(vl <= 1, "A Sentinel detection management only supports the vls 0 (no detection) and 1 (detection).");
        detectionSet.add(player.getUniqueId());
    }
}
