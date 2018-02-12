package de.photon.AACAdditionPro.checks.subchecks.clientcontrol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
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
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Wither;

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
        if (entity instanceof LivingEntity &&
            // Not the player himself.
            // Offline mode servers have name-based UUIDs, so that should pose no problem.
            !event.getPlayer().getUniqueId().equals(entity.getUniqueId()))
        {
            final LivingEntity livingEntity = (LivingEntity) entity;

            // Should spoof?
            if (!livingEntity.isDead() &&
                // Bossbar problems
                !(livingEntity instanceof Wither) &&
                !(livingEntity instanceof EnderDragon) &&
                // Categories
                (livingEntity instanceof HumanEntity && spoofPlayers) ||
                (livingEntity instanceof Monster && spoofMonsters) ||
                (livingEntity instanceof Animals) && spoofAnimals)
            {
                final List<WrappedWatchableObject> wrappedWatchableObjects = entityMetadataWrapper.getMetadata();

                // Index of the health value in ENTITY_METADATA
                final int index;

                // Add spoofed health
                switch (ServerVersion.getActiveServerVersion())
                {
                    case MC188:
                        // index 6 in 1.8
                        index = 6;
                        break;

                    case MC110:
                    case MC111:
                    case MC112:
                        // index 7 in 1.10+
                        // Remove original health.
                        index = 7;
                        break;
                    default:
                        throw new IllegalStateException("Unknown minecraft version");
                }

                // Set health
                for (WrappedWatchableObject wrappedWatchableObject : wrappedWatchableObjects)
                {
                    if (wrappedWatchableObject.getIndex() == index)
                    {
                        wrappedWatchableObject.setValue(20F, false);
                        break;
                    }
                }

                // Set the new metadata.
                entityMetadataWrapper.setMetadata(wrappedWatchableObjects);
                System.out.print("Modified metadata: " + livingEntity.getName() + " | " + event.getPlayer().getName());
            }
        }
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.DAMAGE_INDICATOR;
    }
}
