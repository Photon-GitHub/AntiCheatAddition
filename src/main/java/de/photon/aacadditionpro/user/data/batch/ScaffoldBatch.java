package de.photon.aacadditionpro.user.data.batch;

import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.datastructure.batch.Batch;
import de.photon.aacadditionpro.util.datastructure.broadcast.Broadcaster;
import lombok.Value;
import lombok.val;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;

public class ScaffoldBatch extends Batch<ScaffoldBatch.TowerBlockPlace>
{
    public static final Broadcaster<Snapshot<TowerBlockPlace>> SCAFFOLD_BATCH_BROADCASTER = new Broadcaster<>();
    private static final int SCAFFOLD_BATCH_SIZE = 6;

    public ScaffoldBatch(@NotNull User user)
    {
        super(SCAFFOLD_BATCH_BROADCASTER, user, SCAFFOLD_BATCH_SIZE, new TowerBlockPlace(null, null, null));
    }

    @Value
    public static class TowerBlockPlace
    {
        long time = System.currentTimeMillis();
        Block block;
        Integer jumpBoostLevel;
        Integer levitationLevel;
        BlockFace blockFace;
        double yaw;
        boolean sneaked;

        public long timeOffset(@NotNull TowerBlockPlace other)
        {
            val otime = other.getTime();
            return time < otime ? otime - time : time - otime;
        }
    }
}
