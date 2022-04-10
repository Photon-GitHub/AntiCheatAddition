package de.photon.anticheataddition.user.data.batch;

import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.datastructure.batch.Batch;
import de.photon.anticheataddition.util.datastructure.broadcast.Broadcaster;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import de.photon.anticheataddition.util.minecraft.world.InternalPotion;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

public class ScaffoldBatch extends Batch<ScaffoldBatch.ScaffoldBlockPlace>
{
    public static final Broadcaster<Snapshot<ScaffoldBlockPlace>> SCAFFOLD_BATCH_BROADCASTER = new Broadcaster<>();
    private static final int SCAFFOLD_BATCH_SIZE = 6;

    public ScaffoldBatch(@NotNull User user)
    {
        super(SCAFFOLD_BATCH_BROADCASTER, user, SCAFFOLD_BATCH_SIZE, new ScaffoldBlockPlace(null, null, new Location(null, 0, 0, 0), true, 1));
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ScaffoldBlockPlace
    {
        long time = System.currentTimeMillis();
        Block block;
        BlockFace blockFace;
        Location location;
        boolean sneaked;
        double speedModifier;

        public ScaffoldBlockPlace(Block block, BlockFace blockFace, User user)
        {
            this(block,
                 blockFace,
                 user.getPlayer().getLocation(),
                 user.hasSneakedRecently(175),
                 InternalPotion.SPEED.getPotionEffect(user.getPlayer()).map(ScaffoldBlockPlace::calcSpeedModifier).orElse(1.0D));
        }

        private static double calcSpeedModifier(@NotNull PotionEffect potionEffect)
        {
            switch (potionEffect.getAmplifier()) {
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

        public long timeOffset(@NotNull ScaffoldBlockPlace other)
        {
            return MathUtil.absDiff(time, other.getTime());
        }
    }
}
