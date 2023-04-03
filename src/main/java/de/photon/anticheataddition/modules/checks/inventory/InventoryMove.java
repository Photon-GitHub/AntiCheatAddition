package de.photon.anticheataddition.modules.checks.inventory;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.minecraft.entity.EntityUtil;
import de.photon.anticheataddition.util.minecraft.tps.TPSProvider;
import de.photon.anticheataddition.util.minecraft.world.InternalPotion;
import de.photon.anticheataddition.util.minecraft.world.MaterialUtil;
import de.photon.anticheataddition.util.minecraft.world.WorldUtil;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;

import java.util.Set;

public final class InventoryMove extends ViolationModule implements Listener
{
    public static final InventoryMove INSTANCE = new InventoryMove();

    private final int cancelVl = loadInt(".cancel_vl", 60);
    private final double minTps = loadDouble(".min_tps", 19.5);
    private final int lenienceMillis = loadInt(".lenience_millis", 0);
    private final int teleportBypassTime = loadInt(".teleport_bypass_time", 900);
    private final int worldChangeBypassTime = loadInt(".world_change_bypass_time", 2000);

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

    private static boolean checkLocationForMaterials(Location location, Set<Material> materials)
    {
        return materials.contains(location.getBlock().getType()) ||
               materials.contains(location.getBlock().getRelative(BlockFace.DOWN).getType());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event)
    {
        final var user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this) || event.getTo() == null ||
            // Check that the player has actually moved.
            // Do this here to prevent bypasses with setting allowedToJump to true
            event.getTo().distanceSquared(event.getFrom()) <= 0.005) return;

        // Not inside a vehicle
        if (user.getPlayer().isInsideVehicle() ||
            // Not flying (may trigger some fps)
            user.getPlayer().isFlying() ||
            // Not using an Elytra
            EntityUtil.INSTANCE.isFlyingWithElytra(user.getPlayer()) ||
            // Player is in an inventory
            !user.hasOpenInventory() ||
            // Player has not been hit recently
            user.getPlayer().getNoDamageTicks() != 0 ||
            // Recent teleports can cause bugs
            user.hasTeleportedRecently(teleportBypassTime) ||
            user.hasChangedWorldsRecently(worldChangeBypassTime) ||
            // Make sure the current chunk of the player is loaded so isInLiquids does not cause async entity world add errors.
            // Test this after user.getInventoryData().hasOpenInventory() to further decrease the chance of async load errors.
            !WorldUtil.INSTANCE.isChunkLoaded(user.getPlayer().getLocation()) ||
            // The player is currently not in a liquid (liquids push)
            user.getHitboxLocation().isInLiquids() ||
            // Auto-Disable if TPS are too low
            !TPSProvider.INSTANCE.atLeastTPS(minTps))
        {
            user.getData().bool.allowedToJump = true;
            return;
        }

        final boolean positiveVelocity = event.getFrom().getY() < event.getTo().getY();
        final boolean noMovement = event.getFrom().getY() == event.getTo().getY();

        // A player is only allowed to jump once.
        // Now that the player is jumping, we can set the flag to false.
        if (positiveVelocity != user.getData().bool.positiveVelocity) {
            if (user.getData().bool.allowedToJump) {
                user.getData().bool.allowedToJump = false;
                return;
            }

            // Bouncing can lead to false positives.
            if (checkLocationForMaterials(event.getFrom(), MaterialUtil.BOUNCE_MATERIALS)) return;

            // Prevent bypasses by checking for positive velocity and the moved distance.
            // Distance is not the same as some packets are sent with 0 distance.
            if ((positiveVelocity || noMovement) &&
                // Jumping onto a stair or slabs false positive
                checkLocationForMaterials(event.getFrom(), MaterialUtil.AUTO_STEP_MATERIALS)) return;

            getManagement().flag(Flag.of(user)
                                     .setAddedVl(20)
                                     .setCancelAction(cancelVl, () -> cancelAction(user, event))
                                     .setDebug(() -> "Inventory-Debug | Player: " + user.getPlayer().getName() + " jumped while having an open inventory."));
            return;
        }

        // Make sure that the last jump is a little ago (same "breaking" effect that needs compensation.)
        if (user.getTimeMap().at(TimeKey.VELOCITY_CHANGE_NO_EXTERNAL_CAUSES).recentlyUpdated(1850) ||
            // No Y change anymore. Anticheat and the rule above makes sure that people cannot jump again.
            // While falling down people can modify their inventories.
            noMovement) return;

        // The break period is longer with the speed effect.
        final long speedMillis = InternalPotion.SPEED.getPotionEffect(user.getPlayer())
                                                     .map(PotionEffect::getAmplifier)
                                                     // If a speed effect exists calculate the speed millis, otherwise the speedMillis are 0.
                                                     .map(amplifier -> Math.max(100, amplifier + 1) * 50L)
                                                     .orElse(0L);

        if (user.notRecentlyOpenedInventory(240L + speedMillis + lenienceMillis) &&
            // Do the entity pushing stuff here (performance impact)
            // No nearby entities that could push the player
            WorldUtil.INSTANCE.getLivingEntitiesAroundEntity(user.getPlayer(), user.getHitboxLocation().hitbox(), 0.1D).isEmpty())
        {
            getManagement().flag(Flag.of(user)
                                     .setAddedVl(5)
                                     .setCancelAction(cancelVl, () -> cancelAction(user, event))
                                     .setDebug(() -> "Inventory-Debug | Player: " + user.getPlayer().getName() + " moved while having an open inventory."));
        }
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(100, 2).build();
    }
}

