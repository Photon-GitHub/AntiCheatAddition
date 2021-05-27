package de.photon.aacadditionpro.modules.additions;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.google.common.collect.ImmutableList;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.exception.UnknownMinecraftException;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ModulePacketAdapter;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.packetwrappers.server.WrapperPlayServerEntityMetadata;
import de.photon.aacadditionpro.util.packetwrappers.server.WrapperPlayServerNamedEntitySpawn;
import de.photon.aacadditionpro.util.world.EntityUtil;
import lombok.val;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Wither;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class DamageIndicator extends Module
{
    /**
     * Index of the health value in ENTITY_METADATA
     */
    public static final int ENTITY_METADATA_HEALTH_FIELD_INDEX;

    static {
        // Passenger problems
        switch (ServerVersion.getActiveServerVersion()) {
            case MC18:
                // index 6 in 1.8
                ENTITY_METADATA_HEALTH_FIELD_INDEX = 6;
                break;
            case MC112:
            case MC113:
                // index 7 in 1.11+
                ENTITY_METADATA_HEALTH_FIELD_INDEX = 7;
                break;
            case MC114:
            case MC115:
            case MC116:
                // index 8 in 1.14.4+
                ENTITY_METADATA_HEALTH_FIELD_INDEX = 8;
                break;
            default:
                throw new UnknownMinecraftException();
        }
    }

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
        val adapter = new DamageIndicatorPacketAdapter(this);
        return ModuleLoader.builder(this)
                           .addPacketListeners(adapter)
                           .build();
    }

    private class DamageIndicatorPacketAdapter extends ModulePacketAdapter
    {
        public DamageIndicatorPacketAdapter(Module module)
        {
            super(module, ListenerPriority.HIGH, ServerVersion.getActiveServerVersion() == ServerVersion.MC18 ?
                                                 // Only register NAMED_ENTITY_SPAWN on 1.8 as it doesn't work on newer versions.
                                                 ImmutableList.of(PacketType.Play.Server.ENTITY_METADATA, PacketType.Play.Server.NAMED_ENTITY_SPAWN) :
                                                 ImmutableList.of(PacketType.Play.Server.ENTITY_METADATA));
        }

        @Override
        public void onPacketSending(PacketEvent event)
        {
            val user = User.safeGetUserFromPacketEvent(event);
            if (User.isUserInvalid(user, this.getModule())) return;

            val entity = event.getPacket().getEntityModifier(event.getPlayer().getWorld()).read(0);

            // Should spoof?
            // Clientside entities will be null in the world's entity list.
            if (entity != null &&
                // Not the player himself.
                // Offline mode servers have name-based UUIDs, so that should be no problem.
                event.getPlayer().getEntityId() != entity.getEntityId() &&
                // Bossbar problems
                // Cannot use Boss interface as that doesn't exist on 1.8.8
                !(entity instanceof EnderDragon) &&
                !(entity instanceof Wither) &&
                // Entity must be living to have health; all categories extend LivingEntity.
                ((entity instanceof HumanEntity && spoofPlayers) ||
                 (entity instanceof Monster && spoofMonsters) ||
                 (entity instanceof Animals && spoofAnimals)) &&
                // Entity has no passengers.
                EntityUtil.getPassengers(entity).isEmpty())
            {

                // Clone the packet to prevent a serversided connection of the health.
                event.setPacket(event.getPacket().deepClone());

                final List<WrappedWatchableObject> read;
                if (event.getPacket().getType() == PacketType.Play.Server.ENTITY_METADATA) {
                    read = new WrapperPlayServerEntityMetadata(event.getPacket()).getMetadata();
                    // Only set it on 1.8.8, otherwise it will just be at the max health.
                    // Automatically excluded on later versions as the PacketType is not registered.
                    // This packetwrapper doesn't currently work with 1.15+.
                } else if (event.getPacketType() == PacketType.Play.Server.NAMED_ENTITY_SPAWN) {
                    read = new WrapperPlayServerNamedEntitySpawn(event.getPacket()).getMetadata().getWatchableObjects();
                } else {
                    throw new IllegalStateException("Unregistered packet type.");
                }

                val spoofedHealth = ServerVersion.getActiveServerVersion() == ServerVersion.MC18 ?
                                    Float.NaN :
                                    (float) Objects.requireNonNull(((LivingEntity) entity).getAttribute(Attribute.GENERIC_MAX_HEALTH), "Tried to get health of entity without health.").getValue();

                spoofHealth(read, spoofedHealth);
            }
        }

        private void spoofHealth(@NotNull final List<WrappedWatchableObject> readMetadata, final float healthToSpoof)
        {
            for (WrappedWatchableObject watch : readMetadata) {
                // Check for the HEALTH field
                if (watch.getIndex() == ENTITY_METADATA_HEALTH_FIELD_INDEX) {
                    // Only set it if the entity is not yet dead to prevent problems on the clientside.
                    if (((Float) watch.getValue() > 0.0F)) watch.setValue(healthToSpoof);
                    // Immediately return to not cause unnecessary reflection calls.
                    return;
                }
            }
        }
    }
}