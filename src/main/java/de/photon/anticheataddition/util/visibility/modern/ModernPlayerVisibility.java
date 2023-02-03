package de.photon.anticheataddition.util.visibility.modern;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.util.datastructure.SetUtil;
import de.photon.anticheataddition.util.visibility.PlayerVisibility;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class ModernPlayerVisibility implements PlayerVisibility, Listener
{
    private static final ItemStack AIR_STACK = new ItemStack(Material.AIR);

    private final SetMultimap<Player, Player> userHiddenFromPlayerMap;


    private final SetMultimap<Player, Player> equipmentHiddenFromPlayerMap;

    public ModernPlayerVisibility()
    {
        userHiddenFromPlayerMap = MultimapBuilder.hashKeys(AntiCheatAddition.SERVER_EXPECTED_PLAYERS)
                                                 .hashSetValues(AntiCheatAddition.WORLD_EXPECTED_PLAYERS)
                                                 .build();
        equipmentHiddenFromPlayerMap = MultimapBuilder.hashKeys(AntiCheatAddition.SERVER_EXPECTED_PLAYERS)
                                                      .hashSetValues(AntiCheatAddition.WORLD_EXPECTED_PLAYERS)
                                                      .build();

        AntiCheatAddition.getInstance().registerListener(this);
    }

    @Override
    public void setHidden(Player observer, Set<Player> fullyHidden, Set<Player> hideEquipment)
    {
        final Set<Player> oldHidden = Set.copyOf(userHiddenFromPlayerMap.get(observer));
        final Set<Player> newlyHidden = SetUtil.difference(fullyHidden, oldHidden);
        final Set<Player> nowRevealed = SetUtil.difference(oldHidden, fullyHidden);

        userHiddenFromPlayerMap.replaceValues(observer, fullyHidden);
        playerHandler(observer, newlyHidden, nowRevealed);

        final Set<Player> oldEquipmentHidden = Set.copyOf(equipmentHiddenFromPlayerMap.get(observer));
        final Set<Player> newlyEquipmentHidden = SetUtil.difference(hideEquipment, oldEquipmentHidden);
        final Set<Player> nowEquipmentRevealed = SetUtil.difference(oldEquipmentHidden, hideEquipment);

        equipmentHiddenFromPlayerMap.replaceValues(observer, hideEquipment);
        equipmentHandler(observer, newlyEquipmentHidden, nowEquipmentRevealed);
    }

    private void equipmentHandler(Player observer, Set<Player> hideEquipment, Set<Player> revealEquipment)
    {
        for (Player userToHide : hideEquipment) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                observer.sendEquipmentChange(userToHide, slot, AIR_STACK);
            }
        }

        for (Player userToReveal : revealEquipment) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                final ItemStack toSend = userToReveal.getInventory().getItem(slot);
                observer.sendEquipmentChange(userToReveal, slot, toSend == null ? AIR_STACK : toSend);
            }
        }
    }

    private void playerHandler(Player observer, Set<Player> hidePlayers, Set<Player> revealPlayers)
    {
        for (Player userToHide : hidePlayers) {
            observer.hidePlayer(AntiCheatAddition.getInstance(), userToHide);
        }

        for (Player userToReveal : revealPlayers) {
            observer.showPlayer(AntiCheatAddition.getInstance(), userToReveal);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event)
    {
        // Creative and Spectator players are ignored by ESP and therefore need to be removed from hiding manually.
        switch (event.getNewGameMode()) {
            case CREATIVE, SPECTATOR -> {
                setHidden(event.getPlayer(), Set.of(), Set.of());
                removePlayer(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event)
    {
        removePlayer(event.getPlayer());
    }

    /**
     * Remove the given user from the underlying map.
     *
     * @param user - the user to remove.
     */
    private void removePlayer(Player user)
    {
        removeFromMap(user, userHiddenFromPlayerMap);
        removeFromMap(user, equipmentHiddenFromPlayerMap);
    }

    private void removeFromMap(Player user, SetMultimap<Player, Player> map)
    {
        map.removeAll(user);
        // Remove all the instances of entity from the values.
        //noinspection StatementWithEmptyBody
        while (map.values().remove(user)) ;
    }
}
