package de.photon.anticheataddition.modules.checks.inventory;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.datastructure.SetUtil;
import de.photon.anticheataddition.util.log.Log;
import de.photon.anticheataddition.util.minecraft.ping.PingProvider;
import de.photon.anticheataddition.util.minecraft.world.WorldUtil;
import de.photon.anticheataddition.util.minecraft.world.entity.EntityUtil;
import de.photon.anticheataddition.util.minecraft.world.entity.InternalPotion;
import de.photon.anticheataddition.util.minecraft.world.material.MaterialUtil;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;

import java.util.Set;
import java.util.stream.Stream;

public final class InventoryMove extends ViolationModule implements Listener
{
    // 300 is the vanilla breaking time without a speed effect (derived from testing, especially slab jumping)
    public static final long BASE_BREAKING_TIME = 300L;


    // The materials below a player that allow a player to jump while having an open inventory.
    private static final Set<Material> JUMP_ENABLING_MATERIALS = Stream.of(MaterialUtil.INSTANCE.getAutoStepMaterials(),
                                                                           MaterialUtil.INSTANCE.getBounceMaterials())
                                                                       .flatMap(Set::stream)
                                                                       .collect(SetUtil.toImmutableEnumSet());

    public static final InventoryMove INSTANCE = new InventoryMove();
    public static final double STANDING_STILL_THRESHOLD = 0.005;
    private final int cancelVl = loadInt(".cancel_vl", 60);
    private final boolean extremePingLeniency = loadBoolean(".extreme_ping_leniency", false);

    private InventoryMove()
    {
        super("Inventory.parts.Move");
    }

    private static void cancelAction(User user, PlayerMoveEvent event)
    {
        // Not many blocks moved to prevent exploits and world change problems.
        if (WorldUtil.INSTANCE.inSameWorld(event.getFrom(), event.getTo()) && event.getFrom().distanceSquared(event.getTo()) < 4) {
            // Teleport back the next tick.
            Bukkit.getScheduler().runTask(AntiCheatAddition.getInstance(), () -> user.getPlayer().teleport(event.getFrom(), PlayerTeleportEvent.TeleportCause.UNKNOWN));
        }
    }

    private static long breakingTime(User user)
    {
        return BASE_BREAKING_TIME + InternalPotion.SPEED.getPotionEffect(user.getPlayer())
                                                        .map(PotionEffect::getAmplifier)
                                                        // If a speed effect exists calculate the speed millis, otherwise the speedMillis are 0.
                                                        .map(amplifier -> Math.max(100, amplifier + 1) * 50L)
                                                        .orElse(0L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event)
    {
        final var user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this) || event.getTo() == null ||
            // Check that the player has actually moved.
            // Do this here to prevent bypasses with setting allowedToJump to true
            event.getTo().distanceSquared(event.getFrom()) <= STANDING_STILL_THRESHOLD) return;

        // Not inside a vehicle
        if (user.getPlayer().isInsideVehicle() ||
            // Not flying (vanilla or elytra) as it may trigger some fps
            user.getPlayer().isFlying() ||
            EntityUtil.INSTANCE.isFlyingWithElytra(user.getPlayer()) ||
            // Player must not be in an inventory
            !user.hasOpenInventory() ||
            // After being hit a player moves due to knock-back, so recent hits can cause false positives.
            user.getPlayer().getNoDamageTicks() != 0 ||
            // Recent teleports can cause bugs
            Inventory.teleportOrWorldChangeBypassed(user) ||
            // The player is currently not in a liquid (liquids push)
            // This would need to check for async chunk loads if done in packets (see history)
            user.isInLiquids() ||
            // Auto-Disable if TPS are too low
            !Inventory.hasMinTPS()) {
            user.getData().bool.allowedToJump = true;
            return;
        }

        final boolean movingUpwards = event.getFrom().getY() < event.getTo().getY();
        final boolean noYMovement = event.getFrom().getY() == event.getTo().getY();

        // Player accelerated upwards.
        if (movingUpwards && !user.getData().bool.movingUpwards) {
            handleJump(user, event);
            return;
        }

        final double yMovement = event.getPlayer().getVelocity().getY();

        Log.finer(() -> "Inventory-Debug | Player " + user.getPlayer().getName() + " checking for falling: " + user.getTimeMap().at(TimeKey.VELOCITY_CHANGE_NO_EXTERNAL_CAUSES).passedTime() + " | y-velocity: " + yMovement);

        // This bypasses players during a long fall from hundreds of blocks.
        if (yMovement < -2.0 ||
            // Bypass a falling player after a normal jump in which they opened an inventory.
            // If the y-movement is 0, the falling process is finished and there is no need for bypassing anymore.
            (user.hasJumpedRecently(1850) && !noYMovement)) {
            user.getTimeMap().at(TimeKey.INVENTORY_MOVE_JUMP_END).update();
            return;
        }

        Log.finer(() -> "Inventory-Debug | Player " + user.getPlayer().getName() + " is not fall-bypassed with y-movement: " + yMovement);

        final long leniency = extremePingLeniency ? Math.min(PingProvider.INSTANCE.getPing(user.getPlayer()) / 2, 500) : 0;
        final long totalBreakingTime = breakingTime(user) + leniency;

        Log.finer(() -> "Inventory-Debug | Player " + user.getPlayer().getName() + " breaking time: " + totalBreakingTime + " (leniency: " + leniency + ")" +
                        " | inventory open passed time: " + user.getTimeMap().at(TimeKey.INVENTORY_OPENED).passedTime() +
                        " | jump end passed time: " + user.getTimeMap().at(TimeKey.INVENTORY_MOVE_JUMP_END).passedTime());

        // The breaking is no longer affecting the user as they have opened their inventory long enough ago.
        if (user.notRecentlyOpenedInventory(totalBreakingTime) &&
            // If the player jumped, we need to check the breaking time after the jump ended.
            user.getTimeMap().at(TimeKey.INVENTORY_MOVE_JUMP_END).notRecentlyUpdated(totalBreakingTime) &&
            // Do the entity pushing stuff here (performance impact)
            // No nearby entities that could push the player
            WorldUtil.INSTANCE.getLivingEntitiesAroundEntity(user.getPlayer(), user.getHitboxLocation().hitbox(), 0.1D).isEmpty()) {
            getManagement().flag(Flag.of(user)
                                     .setAddedVl(5)
                                     .setCancelAction(cancelVl, () -> cancelAction(user, event))
                                     .setDebug(() -> "Inventory-Debug | Player: " + user.getPlayer().getName() + " moved while having an open inventory."));
        }
    }

    private void handleJump(User user, PlayerMoveEvent event)
    {
        Log.finer(() -> "Inventory-Debug | Player " + user.getPlayer().getName() + " detected a jump.");

        // A player is only allowed to jump once.
        if (user.getData().bool.allowedToJump) {
            user.getData().bool.allowedToJump = false;
            return;
        }

        Log.finer(() -> "Inventory-Debug | Player " + user.getPlayer().getName() + " is not jump-bypassed.");

        // Bouncing can lead to false positives.
        if (playerCanJumpDueToGroundMaterial(event.getFrom())) return;

        Log.finer(() -> "Inventory-Debug | Player " + user.getPlayer().getName() + " no jump enabling materials detected.");

        getManagement().flag(Flag.of(user)
                                 .setAddedVl(25)
                                 .setCancelAction(cancelVl, () -> cancelAction(user, event))
                                 .setDebug(() -> "Inventory-Debug | Player: " + user.getPlayer().getName() + " jumped while having an open inventory."));
    }

    /**
     * This checks all 9 blocks centered on where the player stands as well as the 9 blocks below to reliably check for materials like slabs.
     * Checking all those blocks is required because stepping up a slab does not mean the player's block-location is already the slab.
     */
    private static boolean playerCanJumpDueToGroundMaterial(Location location)
    {
        return Stream.concat(WorldUtil.INSTANCE.getBlocksAround(location.getBlock(), WorldUtil.HORIZONTAL_FACES, Set.of()).stream(),
                             WorldUtil.INSTANCE.getBlocksAround(location.getBlock().getRelative(BlockFace.DOWN), WorldUtil.HORIZONTAL_FACES, Set.of()).stream())
                     .map(Block::getType)
                     .anyMatch(InventoryMove.JUMP_ENABLING_MATERIALS::contains);
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(100, 2).build();
    }
}

