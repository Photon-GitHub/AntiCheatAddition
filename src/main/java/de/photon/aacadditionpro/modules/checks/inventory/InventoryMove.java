package de.photon.aacadditionpro.modules.checks.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.protocol.PacketAdapterBuilder;
import de.photon.aacadditionpro.protocol.packetwrappers.IWrapperPlayPosition;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.DataKey;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.mathematics.Hitbox;
import de.photon.aacadditionpro.util.messaging.DebugSender;
import de.photon.aacadditionpro.util.minecraft.entity.EntityUtil;
import de.photon.aacadditionpro.util.minecraft.tps.TPSProvider;
import de.photon.aacadditionpro.util.minecraft.world.WorldUtil;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import lombok.Getter;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import java.util.concurrent.ExecutionException;

public class InventoryMove extends ViolationModule
{
    @Getter
    private static final InventoryMove instance = new InventoryMove();

    @LoadFromConfiguration(configPath = ".cancel_vl")
    @Getter
    private int cancelVl;

    @LoadFromConfiguration(configPath = ".min_tps")
    private double minTps;
    @LoadFromConfiguration(configPath = ".lenience_millis")
    private int lenienceMillis;
    @LoadFromConfiguration(configPath = ".teleport_bypass_time")
    private int teleportBypassTime;
    @LoadFromConfiguration(configPath = ".world_change_bypass_time")
    private int worldChangeBypassTime;

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
            Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> user.getPlayer().teleport(knownPosition, PlayerTeleportEvent.TeleportCause.UNKNOWN));
        }
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
                        // Make sure the current chunk of the player is loaded so the liquids method does not cause async entity
                        // world add errors.
                        // Test this after user.getInventoryData().hasOpenInventory() to further decrease the chance of async load
                        // errors.
                        WorldUtil.INSTANCE.isChunkLoaded(user.getPlayer().getLocation()) &&
                        // The player is currently not in a liquid (liquids push)
                        !Hitbox.PLAYER.isInLiquids(knownPosition) &&
                        // Auto-Disable if TPS are too low
                        TPSProvider.INSTANCE.getTPS() > minTps)
                    {
                        val positiveVelocity = knownPosition.getY() < moveTo.getY();

                        if (positiveVelocity != user.getDataMap().getBoolean(DataKey.BooleanKey.POSITIVE_VELOCITY)) {
                            if (user.getDataMap().getBoolean(DataKey.BooleanKey.ALLOWED_TO_JUMP)) {
                                user.getDataMap().setBoolean(DataKey.BooleanKey.ALLOWED_TO_JUMP, false);
                                return;
                            }

                            getManagement().flag(Flag.of(user)
                                                     .setAddedVl(20)
                                                     .setCancelAction(cancelVl, () -> cancelAction(user, event))
                                                     .setEventNotCancelledAction(() -> DebugSender.getInstance().sendDebug("Inventory-Debug | Player: " + user.getPlayer().getName() + " jumped while having an open inventory.")));
                            return;
                        }

                        // Make sure that the last jump is a little bit ago (same "breaking" effect that needs compensation.)
                        if (user.getTimestampMap().at(TimestampKey.LAST_VELOCITY_CHANGE_NO_EXTERNAL_CAUSES).recentlyUpdated(1850)) return;

                        // No Y change anymore. AAC and the rule above makes sure that people cannot jump again.
                        // While falling down people can modify their inventories.
                        if (knownPosition.getY() == moveTo.getY() &&
                            // 230 is a little compensation for the "breaking" when sprinting previously (value has been established
                            // by local tests).
                            user.notRecentlyOpenedInventory(240L + lenienceMillis))
                        {
                            // Do the entity pushing stuff here (performance impact)
                            // No nearby entities that could push the player
                            try {
                                // Needs to be called synchronously.
                                if (Boolean.TRUE.equals(Bukkit.getScheduler().callSyncMethod(AACAdditionPro.getInstance(), () -> WorldUtil.INSTANCE.getLivingEntitiesAroundEntity(user.getPlayer(), user.getHitbox(), 0.1D).isEmpty()).get())) {
                                    getManagement().flag(Flag.of(user)
                                                             .setAddedVl(5)
                                                             .setCancelAction(cancelVl, () -> cancelAction(user, event))
                                                             .setEventNotCancelledAction(() -> DebugSender.getInstance().sendDebug("Inventory-Debug | Player: " + user.getPlayer().getName() + " moved while having an open inventory.")));
                                }
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } catch (ExecutionException e) {
                                // Ignore
                            }
                        }
                    } else {
                        user.getDataMap().setBoolean(DataKey.BooleanKey.ALLOWED_TO_JUMP, true);
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

