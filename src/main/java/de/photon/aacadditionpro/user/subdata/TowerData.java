package de.photon.aacadditionpro.user.subdata;

import de.photon.aacadditionpro.modules.checks.tower.Tower;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.subdata.datawrappers.TowerBlockPlace;
import de.photon.aacadditionpro.util.datastructures.batch.Batch;
import lombok.Getter;

public class TowerData extends SubData
{
    // Default buffer size is 6, being well tested.
    public static final int TOWER_BATCH_SIZE = 6;

    @Getter
    private final Batch<TowerBlockPlace> batch;

    public TowerData(final User user)
    {
        super(user);

        batch = new Batch<>(user, TOWER_BATCH_SIZE, new TowerBlockPlace(user.getPlayer().getEyeLocation().getBlock(), null, null));
        batch.registerProcessor(Tower.getInstance().getTowerBatchProcessor());
    }
}
