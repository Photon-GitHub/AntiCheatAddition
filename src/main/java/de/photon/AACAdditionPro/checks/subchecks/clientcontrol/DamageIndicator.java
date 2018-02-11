package de.photon.AACAdditionPro.checks.subchecks.clientcontrol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.Module;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.multiversion.ServerVersion;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityMetadata;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;

import java.util.List;

public class DamageIndicator extends PacketAdapter implements Module
{
    @LoadFromConfiguration(configPath = ".spoof.players")
    private boolean spoofPlayers;
    @LoadFromConfiguration(configPath = ".spoof.animals")
    private boolean spoofAnimals;
    @LoadFromConfiguration(configPath = ".spoof.monsters")
    private boolean spoofMonsters;

    public DamageIndicator()
    {
        super(AACAdditionPro.getInstance(), ListenerPriority.HIGH, PacketType.Play.Server.ENTITY_METADATA);
    }

    @Override
    public void onPacketSending(PacketEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user))
        {
            return;
        }

        final WrapperPlayServerEntityMetadata entityMetadataWrapper = new WrapperPlayServerEntityMetadata(event.getPacket());
        final Entity entity = entityMetadataWrapper.getEntity(event);

        // Entity must be living to have health.
        if (entity instanceof LivingEntity)
        {
            final LivingEntity livingEntity = (LivingEntity) entity;

            // Should spoof?
            if (!livingEntity.isDead() &&
                (livingEntity instanceof HumanEntity && spoofPlayers) ||
                (livingEntity instanceof Monster && spoofMonsters) ||
                (livingEntity instanceof Animals) && spoofAnimals)
            {
                final List<WrappedWatchableObject> wrappedWatchableObjects = entityMetadataWrapper.getMetadata();

                // Remove original health.
                wrappedWatchableObjects.removeIf(wrappedWatchableObject -> wrappedWatchableObject.getIndex() == 7);

                // Add spoofed health
                switch (ServerVersion.getActiveServerVersion())
                {
                    case MC188:
                        // index 6 in 1.8
                        wrappedWatchableObjects.add(new WrappedWatchableObject(6, 20F));
                        break;

                    case MC110:
                    case MC111:
                    case MC112:
                        // index 7 in 1.10+
                        final WrappedDataWatcher.WrappedDataWatcherObject healthWatcher = new WrappedDataWatcher.WrappedDataWatcherObject(7, WrappedDataWatcher.Registry.get(Float.class));
                        wrappedWatchableObjects.add(new WrappedWatchableObject(healthWatcher, 20F));
                        break;
                    default:
                        throw new IllegalStateException("Unknown minecraft version");
                }

                // Set the new metadata.
                entityMetadataWrapper.setMetadata(wrappedWatchableObjects);
            }
        }
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.DAMAGE_INDICATOR;
    }
}
