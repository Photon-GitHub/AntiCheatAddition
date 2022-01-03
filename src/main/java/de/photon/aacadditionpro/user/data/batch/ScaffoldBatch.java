package de.photon.aacadditionpro.user.data.batch;

import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.datastructure.batch.Batch;
import de.photon.aacadditionpro.util.datastructure.broadcast.Broadcaster;
import de.photon.aacadditionpro.util.mathematics.MathUtil;
import de.photon.aacadditionpro.util.minecraft.world.InternalPotion;
import lombok.Value;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;

public class ScaffoldBatch extends Batch<ScaffoldBatch.ScaffoldBlockPlace>
{
    public static final Broadcaster<Snapshot<ScaffoldBlockPlace>> SCAFFOLD_BATCH_BROADCASTER = new Broadcaster<>();
    private static final int SCAFFOLD_BATCH_SIZE = 6;

    public ScaffoldBatch(@NotNull User user)
    {
        super(SCAFFOLD_BATCH_BROADCASTER, user, SCAFFOLD_BATCH_SIZE, new ScaffoldBlockPlace(null, null, InternalPotion.PotentialPotionEffect.EMPTY, new Location(null, 0, 0, 0), false));
    }

    @Value
    public static class ScaffoldBlockPlace
    {
        long time = System.currentTimeMillis();
        Block block;
        BlockFace blockFace;
        InternalPotion.PotentialPotionEffect speed;
        Location location;
        boolean sneaked;

        public long timeOffset(@NotNull ScaffoldBlockPlace other)
        {
            return MathUtil.absDiff(time, other.getTime());
        }

        public double getSpeedModifier()
        {
            if (!speed.exists()) return 1.0D;

            switch (speed.getAmplifier()) {
                case 0:
                    return 1.01D;
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                    return 1.5D;
                case 6:
                    return 1.55D;
                case 7:
                    return 2.3D;
                default:
                    // Everything above 8 should have a speed_modifier of 3
                    return 3.0D;
            }
        }
    }
}
