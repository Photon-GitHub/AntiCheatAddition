package de.photon.anticheataddition.modules.checks.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.protocol.packetwrappers.IWrapperPlayPosition;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.DataKey;
import de.photon.anticheataddition.user.data.TimestampKey;
import de.photon.anticheataddition.util.minecraft.entity.EntityUtil;
import de.photon.anticheataddition.util.minecraft.tps.TPSProvider;
import de.photon.anticheataddition.util.minecraft.world.InternalPotion;
import de.photon.anticheataddition.util.minecraft.world.MaterialUtil;
import de.photon.anticheataddition.util.minecraft.world.WorldUtil;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import lombok.Getter;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import java.util.Set;

public class InventoryMove extends ViolationModule
{
    private final int cancelVl = loadInt(".cancel_vl", 60);
    private final double minTps = loadDouble(".min_tps", 19.5);
    private final int lenienceMillis = loadInt(".lenience_millis", 0);
    private final int teleportBypassTime = loadInt(".teleport_bypass_time", 900);
    private final int worldChangeBypassTime = loadInt(".world_change_bypass_time", 2000);

    public InventoryMove()
    {
        super("Inventory.parts.Move");
    }

    private static void cancelAction(User user, PacketEvent packetEvent)
    {
        final IWrapperPlayPosition positionWrapper = packetEvent::getPacket;
        val knownPosition = user.getPlayer().getLocation();

        // Not many blocks moved to prevent exploits and world change problems.
        if (positionWrapper.getPosition().distanceSquared(knownPosition.toVector()) < 4) {
            // Teleport back the next tick.
            Bukkit.getScheduler().runTask(AntiCheatAddition.getInstance(), () -> user.getPlayer().teleport(knownPosition, PlayerTeleportEvent.TeleportCause.UNKNOWN));
        }
    }

    private boolean checkLocationForMaterials(Location location, Set<Material> materials)
    {
        return materials.contains(location.getBlock().getType()) ||
               materials.contains(location.getBlock().getRelative(BlockFace.DOWN).getType());
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        val packetAdapter = PacketAdapterBuilder
                // Look
                .of(PacketType.Play.Client.LOOK,
                    // Move
                    PacketType.Play.Client.POSITION,
                    PacketType.Play.Client.POSITION_LOOK)
                .priority(ListenerPriority.LOWEST)
                .onReceiving(event -> {
                    val user = User.safeGetUserFromPacketEvent(event);
                    if (User.isUserInvalid(user, this)) return;

                    final IWrapperPlayPosition positionWrapper = event::getPacket;

                    val moveTo = new Vector(positionWrapper.getX(),
                                            positionWrapper.getY(),
                                            positionWrapper.getZ());

                    val knownPosition = user.getPlayer().getLocation();

                    // Check if this is a clientside movement:
                    // Position Vectors are not the same
                    if (!moveTo.equals(knownPosition.toVector()) &&
                        // Not inside a vehicle
                        !user.getPlayer().isInsideVehicle() &&
                        // Not flying (may trigger some fps)
                        !user.getPlayer().isFlying() &&
                        // Not using an Elytra
                        !EntityUtil.INSTANCE.isFlyingWithElytra(user.getPlayer()) &&
                        // Player is in an inventory
                        user.hasOpenInventory() &&
                        // Player has not been hit recently
                        user.getPlayer().getNoDamageTicks() == 0 &&
                        // Recent teleports can cause bugs
                        !user.hasTeleportedRecently(teleportBypassTime) &&
                        !user.hasChangedWorldsRecently(worldChangeBypassTime) &&
                        // Make sure the current chunk of the player is loaded so isInLiquids does not cause async entity world add errors.
                        // Test this after user.getInventoryData().hasOpenInventory() to further decrease the chance of async load errors.
                        WorldUtil.INSTANCE.isChunkLoaded(user.getPlayer().getLocation()) &&
                        // The player is currently not in a liquid (liquids push)
                        !user.getHitbox().isInLiquids(knownPosition) &&
                        // Auto-Disable if TPS are too low
                        TPSProvider.INSTANCE.atLeastTPS(minTps))
                    {
                        val positiveVelocity = knownPosition.getY() < moveTo.getY();
                        val noMovement = knownPosition.getY() == moveTo.getY();

                        if (positiveVelocity != user.getDataMap().getBoolean(DataKey.Bool.POSITIVE_VELOCITY)) {
                            if (user.getDataMap().getBoolean(DataKey.Bool.ALLOWED_TO_JUMP)) {
                                user.getDataMap().setBoolean(DataKey.Bool.ALLOWED_TO_JUMP, false);
                                return;
                            }

                            // Bouncing can lead to false positives.
                            if (checkLocationForMaterials(knownPosition, MaterialUtil.BOUNCE_MATERIALS)) return;

                            // Prevent bypasses by checking for positive velocity and the moved distance.
                            // Distance is not the same as some packets are sent with 0 distance.
                            if ((positiveVelocity || noMovement) &&
                                // Jumping onto a stair or slabs false positive
                                checkLocationForMaterials(knownPosition, MaterialUtil.AUTO_STEP_MATERIALS)) return;

                            getManagement().flag(Flag.of(user)
                                                     .setAddedVl(20)
                                                     .setCancelAction(cancelVl, () -> cancelAction(user, event))
                                                     .setDebug("Inventory-Debug | Player: " + user.getPlayer().getName() + " jumped while having an open inventory."));
                            return;
                        }

                        // Make sure that the last jump is a little ago (same "breaking" effect that needs compensation.)
                        if (user.getTimestampMap().at(TimestampKey.LAST_VELOCITY_CHANGE_NO_EXTERNAL_CAUSES).recentlyUpdated(1850) ||
                            // No Y change anymore. Anticheat and the rule above makes sure that people cannot jump again.
                            // While falling down people can modify their inventories.
                            knownPosition.getY() == moveTo.getY()) return;

                        // The break period is longer with the speed effect.
                        final long speedMillis = InternalPotion.SPEED.getPotionEffect(user.getPlayer())
                                                                     .map(PotionEffect::getAmplifier)
                                                                     // If a speed effect exists calculate the speed millis, otherwise the speedMillis are 0.
                                                                     .map(amplifier -> Math.max(100, amplifier + 1) * 50L)
                                                                     .orElse(0L);

                        if (user.notRecentlyOpenedInventory(240L + speedMillis + lenienceMillis) &&
                            // Do the entity pushing stuff here (performance impact)
                            // No nearby entities that could push the player
                            PacketAdapterBuilder.checkSync(() -> WorldUtil.INSTANCE.getLivingEntitiesAroundEntity(user.getPlayer(), user.getHitbox(), 0.1D).isEmpty()))
                        {
                            getManagement().flag(Flag.of(user)
                                                     .setAddedVl(5)
                                                     .setCancelAction(cancelVl, () -> cancelAction(user, event))
                                                     .setDebug("Inventory-Debug | Player: " + user.getPlayer().getName() + " moved while having an open inventory."));
                        }
                    } else {
                        user.getDataMap().setBoolean(DataKey.Bool.ALLOWED_TO_JUMP, true);
                    }
                }).build();

        return ModuleLoader.builder(this)
                           .addPacketListeners(packetAdapter)
                           .build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(100, 2).build();
    }
}

