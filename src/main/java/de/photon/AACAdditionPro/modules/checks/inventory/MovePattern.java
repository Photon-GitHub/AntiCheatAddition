package de.photon.AACAdditionPro.modules.checks.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.util.VerboseSender;
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

class MovePattern extends PatternModule.Pattern<User, PacketEvent>
{
    @LoadFromConfiguration(configPath = ".cancel_vl")
    @Getter
    private static int cancelVl;

    @LoadFromConfiguration(configPath = ".min_tps")
    private double min_tps;
    @LoadFromConfiguration(configPath = ".lenience_millis")
    private int lenience_millis;

    @Override
    protected int process(User user, PacketEvent packetEvent)
    {
        if (packetEvent.getPacketType() == PacketType.Play.Client.POSITION ||
            packetEvent.getPacketType() == PacketType.Play.Client.POSITION_LOOK)
        {
            final IWrapperPlayPosition positionWrapper = packetEvent::getPacket;
            // Get motX and motZ
            // final Object nmsHandle = Reflect.fromOBC("entity.CraftPlayer").method("getHandle").invoke(user.getPlayer());

            // motX and motZ are not 0 if the player is collided accordingly.
            // final double motX = Reflect.fromNMS("Entity").field("motX").from(nmsHandle).asDouble();
            //final double motZ = Reflect.fromNMS("Entity").field("motZ").from(nmsHandle).asDouble();


            final Vector moveTo = new Vector(positionWrapper.getX(),
                                             positionWrapper.getY(),
                                             positionWrapper.getZ());

            final Location knownPosition = user.getPlayer().getLocation();

            // Check if this is a clientside movement:
            // Position Vectors are not the same
            if (!moveTo.equals(knownPosition.toVector()) &&
                // or movement is not 0
                //motX != 0 &&
                // motZ != 0 &&
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
                // Open inventory while jumping is covered by the safe-time and the fall distance
                // This covers the big jumps
                final boolean currentlyNotJumping = (user.getPlayer().getVelocity().getY() <= 0 && user.getPlayer().getFallDistance() == 0);

                // Not allowed to start another jump in the inventory
                if (currentlyNotJumping) {
                    user.getPositionData().allowedToJump = false;
                }

                // If the player is jumping and is allowed to jump the max. time is 500 (478.5 is the legit jump time regarding the Tower check).
                // Otherwise it is 100 (little compensation for the "breaking" when sprinting previously
                final int allowedRecentlyOpenedTime = (currentlyNotJumping ?
                                                       100 :
                                                       user.getPositionData().allowedToJump ?
                                                       500 :
                                                       100) + lenience_millis;

                // Was already in inventory or no air - movement (fall distance + velocity)
                if (user.getInventoryData().notRecentlyOpened(allowedRecentlyOpenedTime) &&
                    // Do the entity pushing stuff here (performance impact)
                    // No nearby entities that could push the player
                    EntityUtil.getLivingEntitiesAroundPlayer(user.getPlayer(), Hitbox.PLAYER, 0.1D).isEmpty())
                {
                    VerboseSender.getInstance().sendVerboseMessage("Inventory-Verbose | Player: " + user.getPlayer().getName() + " moved while having an open inventory.");
                    return 3;
                }
            }
            else {
                user.getPositionData().allowedToJump = true;
            }
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

