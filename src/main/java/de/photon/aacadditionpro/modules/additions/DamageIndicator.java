package de.photon.aacadditionpro.modules.additions;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.protocol.EntityMetadataIndex;
import de.photon.aacadditionpro.protocol.PacketAdapterBuilder;
import de.photon.aacadditionpro.protocol.packetwrappers.MetadataPacket;
import de.photon.aacadditionpro.protocol.packetwrappers.sentbyserver.WrapperPlayServerEntityMetadata;
import de.photon.aacadditionpro.protocol.packetwrappers.sentbyserver.WrapperPlayServerNamedEntitySpawn;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.world.EntityUtil;
import lombok.val;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Monster;

public class DamageIndicator extends Module
{
    @LoadFromConfiguration(configPath = ".spoof.players")
    private boolean spoofPlayers;
    @LoadFromConfiguration(configPath = ".spoof.animals")
    private boolean spoofAnimals;
    @LoadFromConfiguration(configPath = ".spoof.monsters")
    private boolean spoofMonsters;

    public DamageIndicator()
    {
        super("DamageIndicator");
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        val packetTypes = ServerVersion.getActiveServerVersion() == ServerVersion.MC18 ?
                          // Only register NAMED_ENTITY_SPAWN on 1.8 as it doesn't work on newer versions.
                          new PacketType[]{PacketType.Play.Server.ENTITY_METADATA, PacketType.Play.Server.NAMED_ENTITY_SPAWN} :
                          new PacketType[]{PacketType.Play.Server.ENTITY_METADATA};

        val adapter = PacketAdapterBuilder
                .of(packetTypes)
                .priority(ListenerPriority.HIGH)
                .onSending(event -> {
                    val user = User.safeGetUserFromPacketEvent(event);
                    if (User.isUserInvalid(user, this)) return;

                    val entity = event.getPacket().getEntityModifier(event.getPlayer().getWorld()).read(0);
                    // Clientside entities will be null in the world's entity list.
                    if (entity == null) return;
                    val entityType = entity.getType();

                    // Should spoof?
                    // Entity has health to begin with.
                    if (entityType.isAlive() &&

                        // Potential performance problems as an armor stand is living for some reason.
                        // TODO: IS THIS ACTUALLY NEEDED?
                        entityType != EntityType.ARMOR_STAND &&

                        // Bossbar problems
                        // Cannot use Boss interface as that doesn't exist on 1.8.8
                        entityType != EntityType.ENDER_DRAGON &&
                        entityType != EntityType.WITHER &&

                        // Entity must be living to have health; all categories extend LivingEntity.
                        ((entity instanceof HumanEntity && spoofPlayers) ||
                         (entity instanceof Monster && spoofMonsters) ||
                         (entity instanceof Animals && spoofAnimals)) &&

                        // Not the player himself.
                        // Offline mode servers have name-based UUIDs, so that should be no problem.
                        event.getPlayer().getEntityId() != entity.getEntityId() &&

                        // Entity has no passengers.
                        EntityUtil.getPassengers(entity).isEmpty())
                    {
                        // Clone the packet to prevent a serversided connection of the health.
                        event.setPacket(event.getPacket().deepClone());

                        final MetadataPacket read;
                        if (event.getPacket().getType() == PacketType.Play.Server.ENTITY_METADATA) {
                            read = new WrapperPlayServerEntityMetadata(event.getPacket());
                            // Only set it on 1.8.8, otherwise it will just be at the max health.
                            // Automatically excluded on later versions as the PacketType is not registered.
                            // This packetwrapper doesn't currently work with 1.15+.
                        } else if (event.getPacketType() == PacketType.Play.Server.NAMED_ENTITY_SPAWN) {
                            read = new WrapperPlayServerNamedEntitySpawn(event.getPacket());
                        } else {
                            throw new IllegalStateException("Unregistered packet type.");
                        }

                        val health = read.getMetadataIndex(EntityMetadataIndex.HEALTH);
                        // Only set it if the entity is not yet dead to prevent problems on the clientside.
                        // Set the health to 1.0F as that is the default value.
                        if (((Float) health.getValue() > 0.0F)) health.setValue(1.0F);
                    }
                }).build();

        return ModuleLoader.builder(this)
                           .addPacketListeners(adapter)
                           .build();
    }
}