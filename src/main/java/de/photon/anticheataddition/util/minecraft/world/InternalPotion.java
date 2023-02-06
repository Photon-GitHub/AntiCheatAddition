package de.photon.anticheataddition.util.minecraft.world;

import de.photon.anticheataddition.ServerVersion;
import lombok.Getter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public enum InternalPotion
{
    /**
     * Increases movement speed.
     */
    SPEED,

    /**
     * Decreases movement speed.
     */
    SLOW,

    /**
     * Increases dig speed.
     */
    FAST_DIGGING,

    /**
     * Decreases dig speed.
     */
    SLOW_DIGGING,

    /**
     * Increases damage dealt.
     */
    INCREASE_DAMAGE,

    /**
     * Heals an entity.
     */
    HEAL,

    /**
     * Hurts an entity.
     */
    HARM,

    /**
     * Increases jump height.
     */
    JUMP,

    /**
     * Warps vision on the client.
     */
    CONFUSION,

    /**
     * Regenerates health.
     */
    REGENERATION,

    /**
     * Decreases damage dealt to an entity.
     */
    DAMAGE_RESISTANCE,

    /**
     * Stops fire damage.
     */
    FIRE_RESISTANCE,

    /**
     * Allows breathing underwater.
     */
    WATER_BREATHING,

    /**
     * Grants invisibility.
     */
    INVISIBILITY,

    /**
     * Blinds an entity.
     */
    BLINDNESS,

    /**
     * Allows an entity to see in the dark.
     */
    NIGHT_VISION,

    /**
     * Increases hunger.
     */
    HUNGER,

    /**
     * Decreases damage dealt by an entity.
     */
    WEAKNESS,

    /**
     * Deals damage to an entity over time.
     */
    POISON,

    /**
     * Deals damage to an entity over time and gives the health to the
     * shooter.
     */
    WITHER,

    /**
     * Increases the maximum health of an entity.
     */
    HEALTH_BOOST,

    /**
     * Increases the maximum health of an entity with health that cannot be
     * regenerated, but is refilled every 30 seconds.
     */
    ABSORPTION,

    /**
     * Increases the food level of an entity each tick.
     */
    SATURATION,

    /**
     * Outlines the entity so that it can be seen from afar.
     */
    GLOWING(ServerVersion.MC19),

    /**
     * Causes the entity to float into the air.
     */
    LEVITATION(ServerVersion.MC19),

    /**
     * Loot table luck.
     */
    LUCK(ServerVersion.MC19),

    /**
     * Loot table unluck.
     */
    UNLUCK(ServerVersion.MC19),

    /**
     * Slows entity fall rate.
     */
    SLOW_FALLING(ServerVersion.MC113),

    /**
     * Effects granted by a nearby conduit. Includes enhanced underwater abilities.
     */
    CONDUIT_POWER(ServerVersion.MC113),

    /**
     * Increases underwater movement speed.<br>
     * Squee'ek uh'k kk'kkkk squeek eee'eek.
     */
    DOLPHINS_GRACE(ServerVersion.MC113),

    /**
     * Triggers a raid when the player enters a village.<br>
     * oof.
     */
    BAD_OMEN(ServerVersion.MC114),

    /**
     * Reduces the cost of villager trades.<br>
     * \o/.
     */
    HERO_OF_THE_VILLAGE(ServerVersion.MC114),

    /**
     * Causes the player's vision to dim occasionally.
     */
    DARKNESS(ServerVersion.MC119);

    private final boolean available;

    @Getter
    private final PotionEffectType mapping;

    InternalPotion()
    {
        this(ServerVersion.MC18);
    }

    InternalPotion(ServerVersion addedInVersion)
    {
        this.available = ServerVersion.containsActive(addedInVersion.getSupVersionsFrom());
        this.mapping = PotionEffectType.getByName(this.name());
    }

    /**
     * This returns all {@link PotionEffectType}s that exist on the active {@link ServerVersion}.
     */
    public static Set<PotionEffectType> getAvailablePotionTypes(InternalPotion... types)
    {
        return Arrays.stream(types).filter(InternalPotion::isAvailable)
                     .map(InternalPotion::getMapping)
                     .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Checks if this PotionEffectType is already available on this ServerVersion.
     */
    public boolean isAvailable()
    {
        return available;
    }

    /**
     * Checks if a {@link LivingEntity} has this potion effect.
     *
     * @param livingEntity the {@link LivingEntity} which should be tested
     *
     * @return true if the potion effect is found, else false.
     */
    public boolean hasPotionEffect(LivingEntity livingEntity)
    {
        return this.isAvailable() && livingEntity.hasPotionEffect(this.getMapping());
    }

    /**
     * Gets a {@link PotionEffect} of a {@link LivingEntity}.
     *
     * @param livingEntity the {@link LivingEntity} which should be tested
     *
     * @return the {@link PotionEffect} with the provided {@link PotionEffectType} or null if the {@link LivingEntity}
     * doesn't have such a {@link PotionEffect}.
     */
    public Optional<PotionEffect> getPotionEffect(@NotNull final LivingEntity livingEntity)
    {
        if (!this.isAvailable()) return Optional.empty();

        return ServerVersion.is18() ?
               // Workaround for missing method in MC 1.8.8
               livingEntity.getActivePotionEffects().stream().filter(pe -> pe.getType().equals(this.mapping)).findAny() :
               Optional.ofNullable(livingEntity.getPotionEffect(this.mapping));
    }
}
