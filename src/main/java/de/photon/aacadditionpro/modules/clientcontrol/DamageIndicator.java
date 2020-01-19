package de.photon.aacadditionpro.modules.clientcontrol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PacketListenerModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.util.entity.EntityUtil;
import de.photon.aacadditionpro.util.exceptions.UnknownMinecraftVersion;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
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
                case MC113:
                    // index 7 in 1.11+
                    index = 7;
                    break;
                case MC114:
                case MC115:
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

            float spoofedHealth;
            switch (ServerVersion.getActiveServerVersion()) {
                case MC188:
                    spoofedHealth = Float.NaN;
                    break;
                case MC113:
                case MC114:
                case MC115:
                    spoofedHealth = (float) Objects.requireNonNull(((LivingEntity) entity).getAttribute(Attribute.GENERIC_MAX_HEALTH), "Tried to get max health of an entity without health.").getValue();
                    break;
                default:
                    throw new UnknownMinecraftVersion();
            }

            final StructureModifier<List<WrappedWatchableObject>> watcher = event.getPacket().getWatchableCollectionModifier();
            if (watcher != null) {
                final List<WrappedWatchableObject> read = watcher.read(0);
                if (read != null) {
                    for (WrappedWatchableObject watch : read) {
                        if ((watch.getIndex() == index) && ((Float) watch.getValue() > 0.0F)) {
                            watch.setValue(spoofedHealth);
                        }
                    }
                }
            }
        }
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.DAMAGE_INDICATOR;
    }
}
