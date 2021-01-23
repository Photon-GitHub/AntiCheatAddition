package de.photon.aacadditionproold.user.subdata;

import com.google.common.base.Preconditions;
import de.photon.aacadditionproold.modules.checks.tower.Tower;
import de.photon.aacadditionproold.user.User;
import de.photon.aacadditionproold.user.subdata.datawrappers.TowerBlockPlace;
import de.photon.aacadditionproold.util.datastructures.batch.Batch;
import lombok.Getter;
import org.bukkit.Bukkit;

public class TowerData extends SubData
{
    // Default buffer size is 6, being well tested.
    public static final int TOWER_BATCH_SIZE = 6;

    @Getter
    private final Batch<TowerBlockPlace> batch;

    public TowerData(final User user)
    {
        super(user);

        batch = new Batch<>(user, TOWER_BATCH_SIZE, new TowerBlockPlace(Preconditions.checkNotNull(Bukkit.getWorlds().get(0), "Scaffold-Batch: No world could be found!").getBlockAt(0, 0, 0), null, null));
        batch.registerProcessor(Tower.getInstance().getBatchProcessor());
    }
}
