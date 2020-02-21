package de.photon.aacadditionpro.modules.additions;

import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.InternalPermission;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.modules.Dependency;
import de.photon.aacadditionpro.modules.ListenerModule;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.RestrictedServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class GuiInject implements ListenerModule, RestrictedServerVersion, Dependency
{
    private static final ItemStack AACADDITIONPRO_ITEMSTACK;
    private static final ItemStack BACK_ITEMSTACK;
    private static final String AACADDITIONPRO_TITLE = ChatColor.GOLD + "AACAdditionPro " + ChatColor.DARK_GRAY + "GUI | Checks";
    public static final String AAC_GUI_TITLE = ChatColor.RED + "AAC " + ChatColor.DARK_GRAY + "GUI | Checks";

    static {
        AACADDITIONPRO_ITEMSTACK = new ItemStack(Material.DIAMOND);

        final ItemMeta meta = AACADDITIONPRO_ITEMSTACK.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY + "AACAdditionPro checks");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "Category contains:");
        lore.add(ChatColor.DARK_GRAY + "");

        for (ModuleType type : ModuleType.values()) {
            lore.add(ChatColor.YELLOW + type.getConfigString());
        }

        lore.add(ChatColor.DARK_GRAY + "");
        lore.add(ChatColor.DARK_GRAY + "HOTKEY: 4");
        meta.setLore(lore);

        AACADDITIONPRO_ITEMSTACK.setItemMeta(meta);


        BACK_ITEMSTACK = new ItemStack(Material.BARRIER);
        final ItemMeta metaBack = BACK_ITEMSTACK.getItemMeta();
        metaBack.setDisplayName(ChatColor.WHITE + "" + ChatColor.ITALIC + "Back to the checks menu");
        BACK_ITEMSTACK.setItemMeta(metaBack);
    }

    @EventHandler
    public void onJoin(InventoryOpenEvent event)
    {
        if (event.getView().getTitle().startsWith(AAC_GUI_TITLE)
            && event.getView().getType() == InventoryType.CHEST
            && InternalPermission.hasPermission(event.getPlayer(), InternalPermission.AAC_MANAGE))
        {
            event.getInventory().setItem(16, AACADDITIONPRO_ITEMSTACK);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event)
    {
        if (event.getView().getType() == InventoryType.CHEST
            && InternalPermission.hasPermission(event.getWhoClicked(), InternalPermission.AAC_MANAGE))
        {
            if (event.getView().getTitle().equals(AAC_GUI_TITLE)
                // Direct click or hotkey.
                && (AACADDITIONPRO_ITEMSTACK.equals(event.getCurrentItem()) || event.getHotbarButton() == 3))
            {
                event.setCancelled(true);

                // closeInventory and openInventory should not be run directly from an ClickEvent handler.
                Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> {
                    event.getWhoClicked().closeInventory();

                    final Inventory shownInventory = Bukkit.createInventory(event.getWhoClicked(), 54, AACADDITIONPRO_TITLE);

                    List<ItemStack> stacks = new ArrayList<>();
                    ItemStack current;
                    for (ModuleType type : ModuleType.values()) {
                        boolean enabled = AACAdditionPro.getInstance().getModuleManager().getStateOfModule(type);

                        current = new ItemStack(enabled ?
                                                Material.GREEN_TERRACOTTA :
                                                Material.GRAY_TERRACOTTA);

                        final ItemMeta meta = current.getItemMeta();
                        meta.setDisplayName(ChatColor.GRAY + type.getConfigString());

                        List<String> lore = new ArrayList<>();
                        lore.add(ChatColor.DARK_GRAY + "Status");
                        lore.add(ChatColor.DARK_GRAY + "");
                        lore.add(ChatColor.GRAY + "Currently this check is " + (enabled ?
                                                                                ChatColor.DARK_GREEN + "enabled" :
                                                                                ChatColor.DARK_RED + "disabled"));

                        meta.setLore(lore);

                        current.setItemMeta(meta);
                        stacks.add(current);
                    }

                    int slot = 0;
                    for (ItemStack checkItem : stacks) {
                        shownInventory.setItem(slot++, checkItem);
                    }

                    shownInventory.setItem(49, BACK_ITEMSTACK);

                    event.getWhoClicked().openInventory(shownInventory);
                });
            }

            if (event.getView().getTitle().equals(AACADDITIONPRO_TITLE)) {
                event.setCancelled(true);

                if (event.getWhoClicked() instanceof Player) {
                    final Player player = (Player) event.getWhoClicked();

                    if (BACK_ITEMSTACK.equals(event.getCurrentItem())) {
                        Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> {
                            player.closeInventory();
                            Bukkit.getServer().dispatchCommand(player, "aac manage");
                        });
                    }

                    if (event.getCurrentItem() != null
                        && (event.getCurrentItem().getType() == Material.GREEN_TERRACOTTA || event.getCurrentItem().getType() == Material.GRAY_TERRACOTTA))
                    {
                        final ItemStack clicked = event.getCurrentItem();
                        final String configPath = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

                        ModuleType toToggle = null;
                        for (ModuleType moduleType : ModuleType.values()) {
                            if (moduleType.getConfigString().equals(configPath)) {
                                toToggle = moduleType;
                                break;
                            }
                        }

                        if (toToggle != null && toToggle != ModuleType.GUI_INJECT) {
                            AACAdditionPro.getInstance().getModuleManager().setStateOfModule(toToggle, !AACAdditionPro.getInstance().getModuleManager().getStateOfModule(toToggle));
                            clicked.setType(AACAdditionPro.getInstance().getModuleManager().getStateOfModule(toToggle) ?
                                            Material.GREEN_TERRACOTTA :
                                            Material.GRAY_TERRACOTTA);

                            event.getInventory().setItem(event.getRawSlot(), clicked);
                            player.updateInventory();
                        }
                    }
                }
            }
        }
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.GUI_INJECT;
    }

    @Override
    public Set<ServerVersion> getSupportedVersions()
    {
        return EnumSet.of(ServerVersion.MC113, ServerVersion.MC114, ServerVersion.MC115);
    }

    @Override
    public Set<String> getDependencies()
    {
        return ImmutableSet.of("AAC");
    }
}
