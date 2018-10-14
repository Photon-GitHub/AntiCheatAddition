package de.photon.AACAdditionPro.util.fakeentity.equipment;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.events.KillauraEntityEquipmentPrepareEvent;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.util.files.configs.ConfigUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class EquipmentDatabase
{
    public static final EquipmentDatabase INSTANCE = new EquipmentDatabase();
    private static final Set<Material> AIR_SET = Collections.singleton(Material.AIR);

    private final Random random = new Random();

    private final EnumMap<EnumWrappers.ItemSlot, Set<Material>> slotMap = new EnumMap<>(EnumWrappers.ItemSlot.class);

    private EquipmentDatabase()
    {
        // -------------------------------------------------- Armor ------------------------------------------------- //
        slotMap.put(EnumWrappers.ItemSlot.HEAD, Sets.newHashSet(
                Material.LEATHER_HELMET,
                Material.GOLDEN_HELMET,
                Material.CHAINMAIL_HELMET,
                Material.IRON_HELMET,
                Material.DIAMOND_HELMET));

        slotMap.put(EnumWrappers.ItemSlot.CHEST, Sets.newHashSet(
                Material.LEATHER_CHESTPLATE,
                Material.GOLDEN_CHESTPLATE,
                Material.CHAINMAIL_CHESTPLATE,
                Material.IRON_CHESTPLATE,
                Material.DIAMOND_CHESTPLATE));

        slotMap.put(EnumWrappers.ItemSlot.LEGS, Sets.newHashSet(
                Material.LEATHER_LEGGINGS,
                Material.GOLDEN_LEGGINGS,
                Material.CHAINMAIL_LEGGINGS,
                Material.IRON_LEGGINGS,
                Material.DIAMOND_LEGGINGS));

        slotMap.put(EnumWrappers.ItemSlot.FEET, Sets.newHashSet(
                Material.LEATHER_BOOTS,
                Material.GOLDEN_BOOTS,
                Material.CHAINMAIL_BOOTS,
                Material.IRON_BOOTS,
                Material.DIAMOND_BOOTS));

        for (final String armorKey : ConfigUtils.loadKeys(ModuleType.KILLAURA_ENTITY.getConfigString() + ".equipment.armor")) {
            // Disabled category
            if (!this.isMaterialAllowed("armor." + armorKey)) {
                final String upCaseArmorKey = armorKey.toUpperCase();
                slotMap.values().forEach(materialList -> materialList.removeIf(material -> material.name().contains(upCaseArmorKey)));
            }
        }

        // ------------------------------------------------- Normal ------------------------------------------------- //
        final Set<Material> handMaterials = new HashSet<>();

        if (this.isMaterialAllowed("normal.raw")) {
            handMaterials.add(Material.BEEF);
            handMaterials.add(Material.CHICKEN);
        }

        if (this.isMaterialAllowed("normal.cooked")) {
            handMaterials.add(Material.COOKED_BEEF);
            handMaterials.add(Material.COOKED_CHICKEN);
            handMaterials.add(Material.COOKED_MUTTON);
            handMaterials.add(Material.COOKED_RABBIT);
        }

        // All other materials where the exact name is in the config.
        for (final String normalKey : ConfigUtils.loadKeys(ModuleType.KILLAURA_ENTITY.getConfigString() + ".equipment.normal")) {
            // Disabled category
            if (this.isMaterialAllowed("normal." + normalKey)) {
                handMaterials.addAll(Arrays.stream(Material.values()).filter(material -> material.name().equalsIgnoreCase(normalKey)).collect(Collectors.toList()));
            }
        }

        if (this.isMaterialAllowed("normal.ingot")) {
            handMaterials.add(Material.GOLD_INGOT);
            handMaterials.add(Material.IRON_INGOT);
        }


        // ------------------------------------------------- Tools -------------------------------------------------- //

        final Set<Material> toolMaterials = Sets.newHashSet(
                // Wood
                Material.WOODEN_AXE,
                Material.WOODEN_HOE,
                Material.WOODEN_PICKAXE,
                Material.WOODEN_SHOVEL,
                Material.WOODEN_SWORD,

                // Gold
                Material.GOLDEN_AXE,
                Material.GOLDEN_HOE,
                Material.GOLDEN_PICKAXE,
                Material.GOLDEN_SHOVEL,
                Material.GOLDEN_SWORD,

                // Stone
                Material.STONE_AXE,
                Material.STONE_HOE,
                Material.STONE_PICKAXE,
                Material.STONE_SHOVEL,
                Material.STONE_SWORD,

                // Iron
                Material.IRON_AXE,
                Material.IRON_HOE,
                Material.IRON_PICKAXE,
                Material.IRON_SHOVEL,
                Material.IRON_SWORD,

                //Diamond
                Material.DIAMOND_AXE,
                Material.DIAMOND_HOE,
                Material.DIAMOND_PICKAXE,
                Material.DIAMOND_SHOVEL,
                Material.DIAMOND_SWORD);

        for (final String toolKey : ConfigUtils.loadKeys(ModuleType.KILLAURA_ENTITY.getConfigString() + ".equipment.tools")) {
            // Disabled category
            if (!this.isMaterialAllowed("tools." + toolKey)) {
                final String upCaseArmorKey = toolKey.toUpperCase();
                slotMap.values().forEach(materialList -> materialList.removeIf(material -> material.name().contains(upCaseArmorKey)));
            }
        }

        handMaterials.addAll(toolMaterials);

        // Set the hands.
        slotMap.put(EnumWrappers.ItemSlot.MAINHAND, handMaterials);
        slotMap.put(EnumWrappers.ItemSlot.OFFHAND, handMaterials);

        // Make all material sets immutable and improve performance.
        slotMap.forEach((slot, materials) -> slotMap.put(slot, ImmutableSet.copyOf(materials)));
    }

    private boolean isMaterialAllowed(final String configName)
    {
        return AACAdditionPro.getInstance().getConfig().getBoolean(ModuleType.KILLAURA_ENTITY.getConfigString() + ".equipment." + configName);
    }

    /**
     * Get an allowed {@link Material} for a certain {@link com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot}
     */
    Material getRandomEquipment(final Player player, final EnumWrappers.ItemSlot itemSlot)
    {
        final Set<Material> possibleMaterials = slotMap.getOrDefault(itemSlot, AIR_SET);

        final KillauraEntityEquipmentPrepareEvent event = new KillauraEntityEquipmentPrepareEvent(player, itemSlot, new ArrayList<>(possibleMaterials));
        Bukkit.getPluginManager().callEvent(event);

        // The boundary parameter of nextInt is exclusive, thus no -1
        return event.getMaterials().get(random.nextInt(event.getMaterials().size()));
    }
}