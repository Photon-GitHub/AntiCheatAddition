package de.photon.anticheataddition.modules.autoeat;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.Dummy;
import de.photon.anticheataddition.events.ViolationEvent;
import de.photon.anticheataddition.modules.checks.autoeat.AutoEat;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.DataUpdaterEvents;
import de.photon.anticheataddition.user.data.TimeKey;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.mockito.Mockito.*;

final class AutoEatTest
{
    private Player player;
    private User user;
    private PlayerInventory inventory;
    private BukkitScheduler scheduler;
    private ItemStack food;

    @BeforeAll
    static void setup()
    {
        Dummy.mockAntiCheatAddition();
    }

    @BeforeEach
    void createUser()
    {
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        food = item(Material.GOLDEN_APPLE);
        when(food.getAmount()).thenReturn(32);
        when(inventory.getItemInMainHand()).thenReturn(food);
        final var air = item(Material.AIR);
        when(inventory.getItemInOffHand()).thenReturn(air);
        user = new User(player);
        scheduler = mock(BukkitScheduler.class);
        when(Bukkit.getScheduler()).thenReturn(scheduler);
    }

    private static ItemStack item(Material material)
    {
        // Modern Material.isEdible delegates to the live Bukkit item registry.
        final var type = mock(Material.class);
        when(type.isEdible()).thenReturn(material == Material.GOLDEN_APPLE);
        final var item = mock(ItemStack.class);
        when(item.getType()).thenReturn(type);
        return item;
    }

    private PlayerItemConsumeEvent consumeEvent()
    {
        final var event = mock(PlayerItemConsumeEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getItem()).thenReturn(food);
        return event;
    }

    private Runnable consume()
    {
        final var event = consumeEvent();
        AutoEat.INSTANCE.onConsume(event);
        DataUpdaterEvents.INSTANCE.onConsume(event);
        // Keep the event order deterministic despite Timestamp's millisecond precision.
        user.getTimeMap().at(TimeKey.CONSUME_EVENT).setToFuture(-1000);
        final var task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTaskLater(eq(AntiCheatAddition.getInstance()), task.capture(), eq(10L));
        return task.getValue();
    }

    private MockedStatic<ViolationEvent> violations()
    {
        final var violations = mockStatic(ViolationEvent.class);
        final var violation = mock(ViolationEvent.class);
        when(violation.call()).thenReturn(violation);
        when(violation.isCancelled()).thenReturn(true);
        violations.when(() -> ViolationEvent.build(player, AutoEat.INSTANCE.getModuleId(), 20)).thenReturn(violation);
        return violations;
    }

    @Test
    void feedingHorseAfterEatingGoldenAppleCountsAsContinuedUse()
    {
        try (var violations = violations()) {
            final var task = consume();
            DataUpdaterEvents.INSTANCE.onItemInteractEntity(new PlayerInteractEntityEvent(player, mock(Horse.class), EquipmentSlot.HAND));
            task.run();
            violations.verifyNoInteractions();
        }
    }

    @Test
    void offhandFoodAndCancelledEntityClicksAlsoShowContinuedUse()
    {
        final var air = item(Material.AIR);
        when(inventory.getItemInMainHand()).thenReturn(air);
        when(inventory.getItemInOffHand()).thenReturn(food);
        try (var violations = violations()) {
            final var task = consume();
            final var event = new PlayerInteractAtEntityEvent(player, mock(Horse.class), new Vector(), EquipmentSlot.OFF_HAND);
            event.setCancelled(true);
            DataUpdaterEvents.INSTANCE.onItemInteractEntity(event);
            task.run();
            violations.verifyNoInteractions();
        }
    }

    @Test
    void absentContinuedUseStillFlags()
    {
        try (var violations = violations()) {
            consume().run();
            violations.verify(() -> ViolationEvent.build(player, AutoEat.INSTANCE.getModuleId(), 20));
        }
    }

    @Test
    void entityClicksWithoutFoodDoNotGrantContinuedUse()
    {
        try (var violations = violations()) {
            final var task = consume();
            final var stone = item(Material.STONE);
            when(inventory.getItemInMainHand()).thenReturn(stone);
            DataUpdaterEvents.INSTANCE.onItemInteractEntity(new PlayerInteractEntityEvent(player, mock(Horse.class), EquipmentSlot.HAND));
            task.run();
            violations.verify(() -> ViolationEvent.build(player, AutoEat.INSTANCE.getModuleId(), 20));
        }
    }

    @Test
    void creativeConsumptionRemainsExempt()
    {
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);
        AutoEat.INSTANCE.onConsume(consumeEvent());
        verifyNoInteractions(scheduler);
    }

    @Test
    void lastItemConsumptionRemainsExempt()
    {
        when(food.getAmount()).thenReturn(1);
        AutoEat.INSTANCE.onConsume(consumeEvent());
        verifyNoInteractions(scheduler);
    }
}
