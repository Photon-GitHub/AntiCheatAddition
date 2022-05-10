package de.photon.anticheataddition.user.data.batch;

import com.google.common.eventbus.EventBus;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.datastructure.batch.Batch;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import de.photon.anticheataddition.util.minecraft.world.InternalPotion;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

public final class ScaffoldBatch extends Batch<ScaffoldBatch.ScaffoldBlockPlace>
{
    public static final EventBus SCAFFOLD_BATCH_EVENTBUS = new EventBus();

    public ScaffoldBatch(@NotNull User user)
    {
        super(SCAFFOLD_BATCH_EVENTBUS, user, 6, new ScaffoldBlockPlace(0, null, null, new Location(null, 0, 0, 0), true, 1));
    }

    public record ScaffoldBlockPlace(long time, Block block, BlockFace blockFace, Location location, boolean sneaked, double speedModifier)
    {
        public ScaffoldBlockPlace(Block block, BlockFace blockFace, User user)
        {
            this(System.currentTimeMillis(),
                 block,
                 blockFace,
                 user.getPlayer().getLocation(),
                 user.hasSneakedRecently(175),
                 InternalPotion.SPEED.getPotionEffect(user.getPlayer()).map(ScaffoldBlockPlace::calcSpeedModifier).orElse(1.0D));
        }

        private static double calcSpeedModifier(@NotNull PotionEffect potionEffect)
        {
            return switch (potionEffect.getAmplifier()) {
                case 0 -> 1.01D;
                case 1, 2, 3, 4, 5 -> 1.5D;
                case 6 -> 1.55D;
                case 7 -> 2.3D;
                // Everything above 8 should have a speed_modifier of 3
                default -> 3.0D;
            };
        }

        public long timeOffset(@NotNull ScaffoldBlockPlace other)
        {
            return MathUtil.absDiff(time, other.time);
        }
    }
}
