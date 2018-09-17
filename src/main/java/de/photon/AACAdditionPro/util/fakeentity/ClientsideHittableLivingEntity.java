package de.photon.AACAdditionPro.util.fakeentity;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.api.killauraentity.Movement;
import de.photon.AACAdditionPro.util.inventory.InventoryUtils;
import de.photon.AACAdditionPro.util.mathematics.Hitbox;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

public class ClientsideHittableLivingEntity extends ClientsideLivingEntity
{
    /**
     * Stores the last timestamp this {@link ClientsideLivingEntity} was hit.
     */
    public long lastHurtMillis;
    private BukkitTask hurtTask = null;

    /**
     * Constructs a new {@link ClientsideLivingEntity}.
     *
     * @param observedPlayer the player that should see this {@link ClientsideLivingEntity}
     * @param hitbox         the {@link Hitbox} of this {@link ClientsideLivingEntity}
     * @param movement       the {@link Movement} of this {@link ClientsideLivingEntity}.
     */
    public ClientsideHittableLivingEntity(Player observedPlayer, Hitbox hitbox, Movement movement)
    {
        super(observedPlayer, hitbox, movement);
    }


    /**
     * Fake being hurt by the observedPlayer in the next sync server tick
     */
    public void hurtByObserved()
    {
        hurtTask = Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> {
            lastHurtMillis = System.currentTimeMillis();
            hurt();

            Location observedLoc = observedPlayer.getLocation();
            observedLoc.setPitch(0);

            // Calculate knockback strength
            int knockbackStrength = observedPlayer.isSprinting() ? 1 : 0;

            // The first index is always the main hand.
            final ItemStack itemInHand = InventoryUtils.getHandContents(observedPlayer).get(0);

            if (itemInHand != null)
            {
                knockbackStrength += itemInHand.getEnchantmentLevel(Enchantment.KNOCKBACK);
            }

            // Apply velocity
            if (knockbackStrength > 0)
            {
                velocity.add(observedLoc.getDirection().normalize().setY(.1).multiply(knockbackStrength * .5));

                //TODO wrong code, its not applied generally, needs to be moved into the method the fake entity hits another entity and apply knockback + sprinting options
//                    motX *= 0.6D;
//                    motZ *= 0.6D;
            }
        });
    }

    /**
     * Fakes the hurt - animation to make the entity look like it was hurt
     */
    private void hurt()
    {
        fakeAnimation(1);
    }

    @Override
    public void despawn()
    {
        super.despawn();

        // Cancel hurt task
        if (hurtTask != null)
        {
            hurtTask.cancel();
        }
    }
}
