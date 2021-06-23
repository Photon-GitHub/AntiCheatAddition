package de.photon.aacadditionpro.user.data.batch;

import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.datastructure.batch.Batch;
import de.photon.aacadditionpro.util.datastructure.broadcast.Broadcaster;
import de.photon.aacadditionpro.util.world.InternalPotion;
import lombok.Value;
import lombok.val;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;

public class ScaffoldBatch extends Batch<ScaffoldBatch.ScaffoldBlockPlace>
{
    public static final Broadcaster<Snapshot<ScaffoldBlockPlace>> SCAFFOLD_BATCH_BROADCASTER = new Broadcaster<>();
    private static final int SCAFFOLD_BATCH_SIZE = 6;

    public ScaffoldBatch(@NotNull User user)
    {
        super(SCAFFOLD_BATCH_BROADCASTER, user, SCAFFOLD_BATCH_SIZE, new ScaffoldBlockPlace(null, null, InternalPotion.PotentialPotionEffect.EMPTY, 0, false));
    }

    @Value
    public static class ScaffoldBlockPlace
    {
        long time = System.currentTimeMillis();
        Block block;
        BlockFace blockFace;
        InternalPotion.PotentialPotionEffect speed;
        double yaw;
        boolean sneaked;

        public long timeOffset(@NotNull ScaffoldBlockPlace other)
        {
            val otime = other.getTime();
            return time < otime ? otime - time : time - otime;
        }
    }
}
