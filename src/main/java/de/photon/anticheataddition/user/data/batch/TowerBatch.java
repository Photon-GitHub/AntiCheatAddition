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
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class TowerBatch extends Batch<TowerBatch.TowerBlockPlace>
{

    public static final Broadcaster<Snapshot<TowerBlockPlace>> TOWER_BATCH_BROADCASTER = new Broadcaster<>();
    private static final int TOWER_BATCH_SIZE = 6;

    public TowerBatch(@NotNull User user)
    {
        super(TOWER_BATCH_BROADCASTER, user, TOWER_BATCH_SIZE, new TowerBlockPlace(new Location(null, 0, 0, 0), Optional.empty(), Optional.empty()));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class TowerBlockPlace
    {
        long time = System.currentTimeMillis();
        Location locationOfBlock;
        Optional<PotionEffect> jumpBoost;
        Optional<PotionEffect> levitation;

        public TowerBlockPlace(Location locationOfBlock, Player player)
        {
            this(locationOfBlock, InternalPotion.JUMP.getPotionEffect(player), InternalPotion.LEVITATION.getPotionEffect(player));
        }

        public long timeOffset(@NotNull TowerBlockPlace other)
        {
            return MathUtil.absDiff(time, other.getTime());
        }
    }
}
