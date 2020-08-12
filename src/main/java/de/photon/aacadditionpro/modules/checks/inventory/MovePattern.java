package de.photon.aacadditionpro.modules.checks.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PacketListenerModule;
import de.photon.aacadditionpro.user.DataKey;
import de.photon.aacadditionpro.user.TimestampKey;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.entity.EntityUtil;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.messaging.VerboseSender;
import de.photon.aacadditionpro.util.packetwrappers.IWrapperPlayPosition;
import de.photon.aacadditionpro.util.server.ServerUtil;
import de.photon.aacadditionpro.util.world.ChunkUtils;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import java.util.concurrent.ExecutionException;

class MovePattern extends PacketAdapter implements PacketListenerModule
{
    @Getter
    private static final MovePattern instance = new MovePattern();

    @LoadFromConfiguration(configPath = ".cancel_vl")
    @Getter
    private int cancelVl;

    @LoadFromConfiguration(configPath = ".min_tps")
    private double minTps;
    @LoadFromConfiguration(configPath = ".lenience_millis")
    private int lenienceMillis;
    @LoadFromConfiguration(configPath = ".teleport_time")
    private int teleportTime;
    @LoadFromConfiguration(configPath = ".world_change_time")
    private int worldChangeTime;

    public MovePattern()
    {
        super(AACAdditionPro.getInstance(), ListenerPriority.LOWEST,
              // Look
              PacketType.Play.Client.LOOK,
              // Move
              PacketType.Play.Client.POSITION,
              PacketType.Play.Client.POSITION_LOOK);
    }

    @Override
    public void onPacketReceiving(PacketEvent event)
    {
        final User user = PacketListenerModule.safeGetUserFromEvent(event);

        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        final IWrapperPlayPosition positionWrapper = event::getPacket;

        final Vector moveTo = new Vector(positionWrapper.getX(),
                                         positionWrapper.getY(),
                                         positionWrapper.getZ());

        final Location knownPosition = user.getPlayer().getLocation();

        // Check if this is a clientside movement:
        // Position Vectors are not the same
        if (!moveTo.equals(knownPosition.toVector()) &&
            // Not inside a vehicle
            !user.getPlayer().isInsideVehicle() &&
            // Not flying (may trigger some fps)
            !user.getPlayer().isFlying() &&
            // Not using an Elytra
            !EntityUtil.isFlyingWithElytra(user.getPlayer()) &&
            // Player is in an inventory
            user.hasOpenInventory() &&
            // Player has not been hit recently
            user.getPlayer().getNoDamageTicks() == 0 &&
            // Recent teleports can cause bugs
            !user.hasTeleportedRecently(this.teleportTime) &&
            !user.hasChangedWorldsRecently(this.worldChangeTime) &&
            // Make sure the current chunk of the player is loaded so the liquids method does not cause async entity
            // world add errors.
            // Test this after user.getInventoryData().hasOpenInventory() to further decrease the chance of async load
            // errors.
            ChunkUtils.isChunkLoaded(user.getPlayer().getLocation()) &&
            // The player is currently not in a liquid (liquids push)
            !EntityUtil.isHitboxInLiquids(knownPosition, user.getHitbox()) &&
            // Auto-Disable if TPS are too low
            ServerUtil.getTPS() > minTps)
        {
            final boolean positiveVelocity = knownPosition.getY() < moveTo.getY();

            if (positiveVelocity != user.getDataMap().getBoolean(DataKey.POSITIVE_VELOCITY)) {
                if (user.getDataMap().getBoolean(DataKey.ALLOWED_TO_JUMP)) {
                    user.getDataMap().setValue(DataKey.ALLOWED_TO_JUMP, false);
                    return;
                }

                Inventory.getInstance().getViolationLevelManagement().flag(user.getPlayer(), 10, this.cancelVl, () -> this.cancelAction(user, event),
                                                                           () -> VerboseSender.getInstance().sendVerboseMessage("Inventory-Verbose | Player: " + user.getPlayer().getName() + " jumped while having an open inventory."));
                return;
            }

            // Make sure that the last jump is a little bit ago (same "breaking" effect that needs compensation.)
            if (user.getTimestampMap().recentlyUpdated(TimestampKey.LAST_VELOCITY_CHANGE_NO_EXTERNAL_CAUSES, 1850)) {
                return;
            }

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
                    if (Boolean.TRUE.equals(Bukkit.getScheduler().callSyncMethod(AACAdditionPro.getInstance(), () -> EntityUtil.getLivingEntitiesAroundEntity(user.getPlayer(), user.getHitbox(), 0.1D).isEmpty()).get())) {
                        Inventory.getInstance().getViolationLevelManagement().flag(user.getPlayer(), 3, this.cancelVl, () -> this.cancelAction(user, event),
                                                                                   () -> VerboseSender.getInstance().sendVerboseMessage("Inventory-Verbose | Player: " + user.getPlayer().getName() + " moved while having an open inventory."));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    // Ignore
                }
            }
        } else {
            user.getDataMap().setValue(DataKey.ALLOWED_TO_JUMP, true);
        }
    }

    private void cancelAction(User user, PacketEvent packetEvent)
    {
        final IWrapperPlayPosition positionWrapper = packetEvent::getPacket;

        final Vector moveTo = new Vector(positionWrapper.getX(),
                                         positionWrapper.getY(),
                                         positionWrapper.getZ());

        final Location knownPosition = user.getPlayer().getLocation();

        // Not many blocks moved to prevent exploits and world change problems.
        if (moveTo.distanceSquared(knownPosition.toVector()) < 4) {
            // Teleport back the next tick.
            Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> user.getPlayer().teleport(knownPosition, PlayerTeleportEvent.TeleportCause.UNKNOWN));
        }
    }

    @Override
    public boolean isSubModule()
    {
        return true;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.Move";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.INVENTORY;
    }
}

