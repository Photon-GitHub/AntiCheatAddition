package de.photon.AACAdditionPro.modules.checks.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.util.entity.EntityUtil;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.mathematics.Hitbox;
import de.photon.AACAdditionPro.util.packetwrappers.IWrapperPlayPosition;
import de.photon.AACAdditionPro.util.packetwrappers.server.WrapperPlayServerPosition;
import de.photon.AACAdditionPro.util.world.ChunkUtils;
import lombok.Getter;
import me.konsolas.aac.api.AACAPIProvider;
import org.bukkit.Location;
import org.bukkit.util.Vector;

class MovePattern extends PatternModule.PacketPattern
{
    @LoadFromConfiguration(configPath = ".cancel_vl")
    @Getter
    private int cancelVl;

    @LoadFromConfiguration(configPath = ".min_tps")
    private double min_tps;
    @LoadFromConfiguration(configPath = ".lenience_millis")
    private int lenience_millis;

    protected MovePattern()
    {
        super(ImmutableSet.of(PacketType.Play.Client.POSITION, PacketType.Play.Client.POSITION_LOOK));
    }

    @Override
    protected int process(User user, PacketEvent packetEvent)
    {

        final IWrapperPlayPosition positionWrapper = packetEvent::getPacket;

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
            user.getInventoryData().hasOpenInventory() &&
            // Player has not been hit recently
            user.getPlayer().getNoDamageTicks() == 0 &&
            // Recent teleports can cause bugs
            !user.getTeleportData().recentlyUpdated(0, 1000) &&
            // Make sure the current chunk of the player is loaded so the liquids method does not cause async entity
            // world add errors.
            // Test this after user.getInventoryData().hasOpenInventory() to further decrease the chance of async load
            // errors.
            ChunkUtils.isChunkLoaded(user.getPlayer().getLocation()) &&
            // The player is currently not in a liquid (liquids push)
            !EntityUtil.isHitboxInLiquids(knownPosition, user.getPlayer().isSneaking() ?
                                                         Hitbox.SNEAKING_PLAYER :
                                                         Hitbox.PLAYER) &&
            // Auto-Disable if TPS are too low
            AACAPIProvider.getAPI().getTPS() > min_tps)
        {
            final boolean positiveVelocity = knownPosition.getY() < moveTo.getY();

            if (positiveVelocity != user.getVelocityChangeData().positiveVelocity) {
                if (user.getPositionData().allowedToJump) {
                    user.getPositionData().allowedToJump = false;
                    return 0;
                }

                message = "Inventory-Verbose | Player: " + user.getPlayer().getName() + " jumped while having an open inventory.";
                return 10;
            }

            // Make sure that the last jump is a little bit ago (same "breaking" effect that needs compensation.)
            if (user.getVelocityChangeData().recentlyUpdated(1, 1750)) {
                return 0;
            }

            // No Y change anymore. AAC and the rule above makes sure that people cannot jump again.
            // While falling down people can modify their inventories.
            if (knownPosition.getY() == moveTo.getY() &&
                // 100 is a little compensation for the "breaking" when sprinting previously
                user.getInventoryData().notRecentlyOpened(100 + lenience_millis) &&
                // Do the entity pushing stuff here (performance impact)
                // No nearby entities that could push the player
                EntityUtil.getLivingEntitiesAroundPlayer(user.getPlayer(), Hitbox.PLAYER, 0.1D).isEmpty())
            {
                message = "Inventory-Verbose | Player: " + user.getPlayer().getName() + " moved while having an open inventory.";
                return 3;
            }
        }
        else {
            user.getPositionData().allowedToJump = true;
        }
        return 0;
    }

    @Override
    public void cancelAction(User user, PacketEvent event)
    {
        //TODO: TEST THIS; THIS MIGHT SEND EMPTY PACKETS ?
        event.setCancelled(true);

        // Update client
        final WrapperPlayServerPosition packet = new WrapperPlayServerPosition();

        //Init with the known values
        packet.setWithLocation(user.getPlayer().getLocation());

        // Set no flags as we do not have a relative movement here.
        packet.setNoFlags();
        packet.sendPacket(event.getPlayer());
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

