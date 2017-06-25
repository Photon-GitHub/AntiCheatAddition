package de.photon.AACAdditionPro.util.clientsideentities.equipment;


import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.util.files.ConfigUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class EntityEquipmentDatabase
{
    private static final ArrayList<Enchantment> enchantments = new ArrayList<>(Arrays.asList(
            // Weapons
            Enchantment.DAMAGE_ALL,
            Enchantment.KNOCKBACK,
            Enchantment.FIRE_ASPECT,
            // Armor
            Enchantment.PROTECTION_ENVIRONMENTAL,
            // Both
            Enchantment.DURABILITY));

    private static final ArrayList<Material> normalMaterials = new ArrayList<>(Arrays.asList(
            // Air
            Material.AIR,
            // Wooden equip
            Material.WOOD_SWORD,
            Material.WOOD_AXE,
            Material.WOOD_PICKAXE,
            Material.WOOD_SPADE,
            Material.WOOD_HOE,

            // Stone equip
            Material.STONE_SWORD,
            Material.STONE_AXE,
            Material.STONE_PICKAXE,
            Material.STONE_SPADE,
            Material.STONE_HOE,

            // Iron Equip
            Material.IRON_SWORD,
            Material.IRON_AXE,
            Material.IRON_PICKAXE,
            Material.IRON_SPADE,
            Material.IRON_HOE,

            // Diamond equip
            Material.DIAMOND_SWORD,
            Material.DIAMOND_AXE,
            Material.DIAMOND_PICKAXE,
            Material.DIAMOND_SPADE,
            Material.DIAMOND_HOE,

            // Other tools
            Material.FLINT_AND_STEEL,
            Material.BOW,
            Material.FISHING_ROD,

            // PvP-stuff (Soups, Pots, etc)
            Material.GOLDEN_APPLE,
            Material.BOWL,
            Material.POTION,
            Material.GLASS_BOTTLE,
            Material.EXP_BOTTLE,

            // Building blocks
            Material.SANDSTONE,
            Material.SANDSTONE_STAIRS,

            // MC-Standard blocks
            Material.DIRT,
            Material.GRASS,
            Material.STONE,
            Material.BEDROCK,
            Material.COAL,
            Material.IRON_ORE,
            Material.GOLD_ORE,

            // Working material
            Material.GOLD_INGOT,
            Material.IRON_INGOT,
            Material.DIAMOND,
            Material.WOOD));

    private static final ArrayList<Material> armorMaterials = new ArrayList<>(Arrays.asList(
            // Leather
            Material.LEATHER_HELMET,
            Material.LEATHER_CHESTPLATE,
            Material.LEATHER_LEGGINGS,
            Material.LEATHER_BOOTS,

            // Gold
            Material.GOLD_HELMET,
            Material.GOLD_CHESTPLATE,
            Material.GOLD_LEGGINGS,
            Material.GOLD_BOOTS,

            // Iron
            Material.IRON_HELMET,
            Material.IRON_CHESTPLATE,
            Material.IRON_LEGGINGS,
            Material.IRON_BOOTS,

            // Diamond
            Material.DIAMOND_HELMET,
            Material.DIAMOND_CHESTPLATE,
            Material.DIAMOND_LEGGINGS,
            Material.DIAMOND_BOOTS));

    private static final ArrayList<Material> weaponMaterials = new ArrayList<>(Arrays.asList(
            Material.WOOD_SWORD,
            Material.WOOD_AXE,
            Material.GOLD_SWORD,
            Material.GOLD_AXE,
            Material.STONE_SWORD,
            Material.STONE_AXE,
            Material.IRON_SWORD,
            Material.IRON_AXE,
            Material.DIAMOND_SWORD,
            Material.DIAMOND_AXE));

    static
    {
        // --------------------------------------------------------------- Weapons --------------------------------------------------------------- //

        // Filter out all kinds (swords, axes) and materials (WOOD, GOLD, STONE, IRON, DIAMOND) that are disabled in the config
        final Set<String> optionKeys = ConfigUtils.loadKeys(AdditionHackType.KILLAURA_ENTITY.getConfigString() + ".equipment.weapons");
        
        for(final String optionKey : optionKeys)
        {
            if(!AACAdditionPro.getInstance().getConfig().getBoolean(AdditionHackType.KILLAURA_ENTITY.getConfigString() + ".equipment.weapons." + optionKey))
            {
                // Filter out swords
                weaponMaterials.removeIf((material -> material.name().contains(optionKey.toUpperCase())));
            }
        }
    }

    /**
     * Gets a {@link Material} a player could possibly have outside of a fight.
     */
    public static Material getRandomNormalMaterial()
    {
        return normalMaterials.get(ThreadLocalRandom.current().nextInt(normalMaterials.size()));
    }

    /**
     * Gets a {@link Material} that represents a weapon a player could possibly use in a fight.
     */
    public static Material getRandomWeaponMaterial()
    {
        return weaponMaterials.get(ThreadLocalRandom.current().nextInt(weaponMaterials.size()));
    }

    /**
     * Gets an Array of {@link Material}s that represents the armor a player could possibly have.
     * [0] == Helmet
     * [1] == Chestplate
     * [2] == Leggings
     * [3] == Boots
     */
    public static Material[] getRandomArmorMaterials()
    {
        final Material[] armor = new Material[4];

        for (byte b = 0; b < (byte) 6; b++) {
            final Material randomMaterial = armorMaterials.get(ThreadLocalRandom.current().nextInt(armorMaterials.size()));

            if (randomMaterial.name().contains("HELMET")) {
                armor[0] = randomMaterial;
            } else if (randomMaterial.name().contains("CHESTPLATE")) {
                armor[1] = randomMaterial;
            } else if (randomMaterial.name().contains("LEGGINGS")) {
                armor[2] = randomMaterial;
            } else {
                armor[3] = randomMaterial;
            }
        }
        return armor;
    }

    public static Enchantment getRandomEnchantment()
    {
        return enchantments.get(ThreadLocalRandom.current().nextInt(enchantments.size()));
    }

}
