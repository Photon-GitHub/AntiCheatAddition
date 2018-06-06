package de.photon.AACAdditionPro.util.fakeentity.equipment;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.google.common.collect.Lists;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.events.KillauraEntityEquipmentPrepareEvent;
import de.photon.AACAdditionPro.util.files.configs.ConfigUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class EquipmentDatabase extends EnumMap<EnumWrappers.ItemSlot, List<Material>>
{
    public static final EquipmentDatabase instance = new EquipmentDatabase();

    private static final List<Material> AIR_LIST = Collections.singletonList(Material.AIR);
    private final Random random = new Random();

    private EquipmentDatabase()
    {
        super(EnumWrappers.ItemSlot.class);

        // -------------------------------------------------- Armor ------------------------------------------------- //
        this.put(EnumWrappers.ItemSlot.HEAD, Lists.newArrayList(
                Material.LEATHER_HELMET,
                Material.GOLD_HELMET,
                Material.CHAINMAIL_HELMET,
                Material.IRON_HELMET,
                Material.DIAMOND_HELMET));

        this.put(EnumWrappers.ItemSlot.CHEST, Lists.newArrayList(
                Material.LEATHER_CHESTPLATE,
                Material.GOLD_CHESTPLATE,
                Material.CHAINMAIL_CHESTPLATE,
                Material.IRON_CHESTPLATE,
                Material.DIAMOND_CHESTPLATE));

        this.put(EnumWrappers.ItemSlot.LEGS, Lists.newArrayList(
                Material.LEATHER_LEGGINGS,
                Material.GOLD_LEGGINGS,
                Material.CHAINMAIL_LEGGINGS,
                Material.IRON_LEGGINGS,
                Material.DIAMOND_LEGGINGS));

        this.put(EnumWrappers.ItemSlot.FEET, Lists.newArrayList(
                Material.LEATHER_BOOTS,
                Material.GOLD_BOOTS,
                Material.CHAINMAIL_BOOTS,
                Material.IRON_BOOTS,
                Material.DIAMOND_BOOTS));

        for (final String armorKey : ConfigUtils.loadKeys(ModuleType.KILLAURA_ENTITY.getConfigString() + ".equipment.armor"))
        {
            // Disabled category
            if (!this.isMaterialAllowed("armor." + armorKey))
            {
                final String upCaseArmorKey = armorKey.toUpperCase();
                this.values().forEach(materialList -> materialList.removeIf(material -> material.name().contains(upCaseArmorKey)));
            }
        }

        // ------------------------------------------------- Normal ------------------------------------------------- //
        final List<Material> handMaterials = new ArrayList<>();

        if (this.isMaterialAllowed("normal.raw"))
        {
            handMaterials.add(Material.RAW_BEEF);
            handMaterials.add(Material.RAW_CHICKEN);
            handMaterials.add(Material.RAW_FISH);
        }

        if (this.isMaterialAllowed("normal.cooked"))
        {
            handMaterials.add(Material.COOKED_BEEF);
            handMaterials.add(Material.COOKED_CHICKEN);
            handMaterials.add(Material.COOKED_FISH);
            handMaterials.add(Material.COOKED_MUTTON);
            handMaterials.add(Material.COOKED_RABBIT);
        }

        // All other materials where the exact name is in the config.
        for (final String normalKey : ConfigUtils.loadKeys(ModuleType.KILLAURA_ENTITY.getConfigString() + ".equipment.normal"))
        {
            // Disabled category
            if (this.isMaterialAllowed("normal." + normalKey))
            {
                handMaterials.addAll(Arrays.stream(Material.values()).filter(material -> material.name().equalsIgnoreCase(normalKey)).collect(Collectors.toList()));
            }
        }

        if (this.isMaterialAllowed("normal.ingot"))
        {
            handMaterials.add(Material.GOLD_INGOT);
            handMaterials.add(Material.IRON_INGOT);
        }


        // ------------------------------------------------- Tools -------------------------------------------------- //

        final List<Material> toolMaterials = Lists.newArrayList(
                // Wood
                Material.WOOD_AXE,
                Material.WOOD_HOE,
                Material.WOOD_PICKAXE,
                Material.WOOD_SPADE,
                Material.WOOD_SWORD,

                // Gold
                Material.GOLD_AXE,
                Material.GOLD_HOE,
                Material.GOLD_PICKAXE,
                Material.GOLD_SPADE,
                Material.GOLD_SWORD,

                // Stone
                Material.STONE_AXE,
                Material.STONE_HOE,
                Material.STONE_PICKAXE,
                Material.STONE_SPADE,
                Material.STONE_SWORD,

                // Iron
                Material.IRON_AXE,
                Material.IRON_HOE,
                Material.IRON_PICKAXE,
                Material.IRON_SPADE,
                Material.IRON_SWORD,

                //Diamond
                Material.DIAMOND_AXE,
                Material.DIAMOND_HOE,
                Material.DIAMOND_PICKAXE,
                Material.DIAMOND_SPADE,
                Material.DIAMOND_SWORD);

        for (final String toolKey : ConfigUtils.loadKeys(ModuleType.KILLAURA_ENTITY.getConfigString() + ".equipment.tools"))
        {
            // Disabled category
            if (!this.isMaterialAllowed("tools." + toolKey))
            {
                final String upCaseArmorKey = toolKey.toUpperCase();
                this.values().forEach(materialList -> materialList.removeIf(material -> material.name().contains(upCaseArmorKey)));
            }
        }

        handMaterials.addAll(toolMaterials);

        // Set the hands.
        this.put(EnumWrappers.ItemSlot.MAINHAND, handMaterials);
        this.put(EnumWrappers.ItemSlot.OFFHAND, handMaterials);
    }

    private boolean isMaterialAllowed(final String configName)
    {
        return AACAdditionPro.getInstance().getConfig().getBoolean(ModuleType.KILLAURA_ENTITY.getConfigString() + ".equipment." + configName);
    }

    /**
     * Get an allowed {@link Material} for a certain {@link com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot}
     */
    public Material getRandomEquipment(final Player player, final EnumWrappers.ItemSlot itemSlot)
    {
        List<Material> possibleMaterials = this.getOrDefault(itemSlot, AIR_LIST);

        final KillauraEntityEquipmentPrepareEvent event = new KillauraEntityEquipmentPrepareEvent(player, itemSlot, possibleMaterials);
        Bukkit.getPluginManager().callEvent(event);

        possibleMaterials = event.getMaterials();

        // The boundary parameter of nextInt is exclusive, thus no -1
        return possibleMaterials.get(random.nextInt(possibleMaterials.size()));
    }
}
