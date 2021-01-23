package de.photon.aacadditionproold.modules.clientcontrol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import de.photon.aacadditionproold.AACAdditionPro;
import de.photon.aacadditionproold.ServerVersion;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.modules.PacketListenerModule;
import de.photon.aacadditionproold.user.User;
import de.photon.aacadditionproold.user.UserManager;
import de.photon.aacadditionproold.util.entity.EntityUtil;
import de.photon.aacadditionproold.util.exceptions.UnknownMinecraftVersion;
import de.photon.aacadditionproold.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionproold.util.packetwrappers.server.WrapperPlayServerEntityMetadata;
import de.photon.aacadditionproold.util.packetwrappers.server.WrapperPlayServerNamedEntitySpawn;
import lombok.Getter;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Wither;

import java.util.List;
import java.util.Objects;

public class DamageIndicator extends PacketAdapter implements PacketListenerModule
{
    @Getter
    private static final DamageIndicator instance = new DamageIndicator();

    @LoadFromConfiguration(configPath = ".spoof.players")
    private boolean spoofPlayers;
    @LoadFromConfiguration(configPath = ".spoof.animals")
    private boolean spoofAnimals;
    @LoadFromConfiguration(configPath = ".spoof.monsters")
    private boolean spoofMonsters;

    public DamageIndicator()
    {
        super(AACAdditionPro.getInstance(), ListenerPriority.HIGH, PacketType.Play.Server.ENTITY_METADATA, PacketType.Play.Server.NAMED_ENTITY_SPAWN);
    }

    @Override
    public void onPacketSending(PacketEvent event)
    {
        if (event.isPlayerTemporary()) {
            return;
        }

        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        final Entity entity = event.getPacket().getEntityModifier(event.getPlayer().getWorld()).read(0);

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
             (entity instanceof Animals && spoofAnimals)))
        {
            // Index of the health value in ENTITY_METADATA
            final int index;

            // Passenger problems
            switch (ServerVersion.getActiveServerVersion()) {
                case MC188:
                    // index 6 in 1.8
                    index = 6;
                    break;
                case MC112:
                case MC113:
                    // index 7 in 1.11+
                    index = 7;
                    break;
                case MC114:
                case MC115:
                case MC116:
                    // index 7 in 1.14.4+
                    index = 8;
                    break;
                default:
                    throw new UnknownMinecraftVersion();
            }

            // No passengers.
            if (!EntityUtil.getPassengers(entity).isEmpty()) {
                return;
            }

            // Clone the packet to prevent a serversided connection of the health.
            event.setPacket(event.getPacket().deepClone());
            List<WrappedWatchableObject> read = null;

            float spoofedHealth;
            switch (ServerVersion.getActiveServerVersion()) {
                case MC188:
                    spoofedHealth = Float.NaN;

                    // Only set it on 1.8.8, otherwise it will just be at the max health.
                    // This packetwrapper doesn't currently work with 1.15+.
                    if (event.getPacket().getType() == PacketType.Play.Server.NAMED_ENTITY_SPAWN) {
                        WrapperPlayServerNamedEntitySpawn wrapper = new WrapperPlayServerNamedEntitySpawn(event.getPacket());
                        read = wrapper.getMetadata().getWatchableObjects();
                    }
                    break;
                case MC112:
                case MC113:
                case MC114:
                case MC115:
                case MC116:
                    spoofedHealth = (float) Objects.requireNonNull(((LivingEntity) entity).getAttribute(Attribute.GENERIC_MAX_HEALTH), "Tried to get max health of an entity without health.").getValue();
                    break;
                default:
                    throw new UnknownMinecraftVersion();
            }

            if (event.getPacket().getType() == PacketType.Play.Server.ENTITY_METADATA) {
                WrapperPlayServerEntityMetadata wrapper = new WrapperPlayServerEntityMetadata(event.getPacket());
                read = wrapper.getMetadata();
            }

            if (read != null) {
                for (WrappedWatchableObject watch : read) {
                    if ((watch.getIndex() == index) && ((Float) watch.getValue() > 0.0F)) {
                        watch.setValue(spoofedHealth);
                    }
                }
            }
        }
    }

    @Override
    public boolean isSubModule()
    {
        return false;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.DAMAGE_INDICATOR;
    }
}
