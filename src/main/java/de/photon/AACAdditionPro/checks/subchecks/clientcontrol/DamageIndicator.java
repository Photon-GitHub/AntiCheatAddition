package de.photon.AACAdditionPro.checks.subchecks.clientcontrol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.Module;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.multiversion.ServerVersion;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
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
        super(AACAdditionPro.getInstance(), ListenerPriority.HIGH, PacketType.Play.Server.ENTITY_METADATA, PacketType.Play.Server.NAMED_ENTITY_SPAWN);
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

        final Entity entity = event.getPacket().getEntityModifier(event.getPlayer().getWorld()).read(0);

        // Should spoof?
        // Not the player himself.
        // Offline mode servers have name-based UUIDs, so that should be no problem.
        if (event.getPlayer().getEntityId() != entity.getEntityId() &&
            // Bossbar problems
            !(entity instanceof Wither) &&
            !(entity instanceof EnderDragon) &&
            // Entity must be living to have health; all categories extend LivingEntity.
            ((entity instanceof HumanEntity && spoofPlayers) ||
             (entity instanceof Monster && spoofMonsters) ||
             (entity instanceof Animals && spoofAnimals)))
        {
            // Index of the health value in ENTITY_METADATA
            final int index;

            // Passenger problems
            switch (ServerVersion.getActiveServerVersion())
            {
                case MC188:
                    // index 6 in 1.8
                    index = 6;

                    if (entity.getPassenger() != null)
                    {
                        return;
                    }
                    break;

                case MC110:
                case MC111:
                case MC112:
                    // index 7 in 1.10+
                    index = 7;

                    if (!entity.getPassengers().isEmpty())
                    {
                        return;
                    }
                    break;
                default:
                    throw new IllegalStateException("Unknown minecraft version");
            }

            // Clone the packet to prevent a serversided connection of the health.
            event.setPacket(event.getPacket().deepClone());

            final StructureModifier<List<WrappedWatchableObject>> watcher = event.getPacket().getWatchableCollectionModifier();
            if (watcher != null)
            {
                List<WrappedWatchableObject> read = watcher.read(0);
                if (read != null)
                {
                    for (WrappedWatchableObject watch : read)
                    {
                        if ((watch.getIndex() == index) && ((Float) watch.getValue() > 0.0F))
                        {
                            watch.setValue(Float.NaN);
                        }
                    }
                }
            }

            System.out.print("Modified metadata: " + entity.getName() + " | " + event.getPlayer().getName());
        }
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.DAMAGE_INDICATOR;
    }
}
