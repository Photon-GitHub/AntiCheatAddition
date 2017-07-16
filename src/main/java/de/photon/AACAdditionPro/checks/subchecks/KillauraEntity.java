package de.photon.AACAdditionPro.checks.subchecks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.entities.ClientsidePlayerEntity;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.storage.management.ViolationLevelManagement;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.StringUtil;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class KillauraEntity implements AACAdditionProCheck, Listener
{
    ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getAdditionHackType(), 300);

    @LoadFromConfiguration(configPath = ".position.entityOffset")
    private double entityOffset;

    @LoadFromConfiguration(configPath = ".position.offsetRandomizationRange")
    private double offsetRandomizationRange;

    @LoadFromConfiguration(configPath = ".position.minXZDifference")
    private double minXZDifference;

    @EventHandler
    public void onPlayerChatTabComplete(final PlayerChatTabCompleteEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (user == null || user.isBypassed()) {
            return;
        }

        final ClientsidePlayerEntity playerEntity = user.getClientSideEntityData().clientSidePlayerEntity;

        if (playerEntity != null &&
            StringUtil.startsWithIgnoreCase(playerEntity.getName(), event.getLastToken()))
        {
            event.getTabCompletions().add(playerEntity.getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerVelocity(final PlayerVelocityEvent event)
    {
        // Add velocity to the bot so the bot does never stand inside or in front of the player
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (user == null || user.isBypassed()) {
            return;
        }

        final ClientsidePlayerEntity playerEntity = user.getClientSideEntityData().clientSidePlayerEntity;

        if (playerEntity != null) {
            playerEntity.setVelocity(event.getVelocity());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent event)
    {
        Bukkit.getScheduler().runTaskLaterAsynchronously(AACAdditionPro.getInstance(), () -> {
            switch (event.getPlayer().getGameMode()) {
                case CREATIVE:
                case SPECTATOR:
                    return;
                case SURVIVAL:
                case ADVENTURE:
                    break;
                default:
                    throw new IllegalStateException("Unknown Gamemode: " + event.getPlayer().getGameMode().name());
            }

            // Add velocity to the bot so the bot does never stand inside or in front of the player
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            // Not bypassed
            if (user == null || user.isBypassed() ||
                // There are valid profiles that can serve the need for entities.
                Bukkit.getOfflinePlayers().length < 1)
            {
                return;
            }

            //TODO: REAL NAMES AND PROFILES
            final OfflinePlayer chosenOfflinePlayer = Bukkit.getOfflinePlayers()[ThreadLocalRandom.current().nextInt(Bukkit.getOfflinePlayers().length)];
            final WrappedGameProfile gameProfile = new WrappedGameProfile(chosenOfflinePlayer.getUniqueId(), chosenOfflinePlayer.getName());

            Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> {
                final ClientsidePlayerEntity playerEntity = new ClientsidePlayerEntity(event.getPlayer(), gameProfile, entityOffset, offsetRandomizationRange, minXZDifference);
                user.getClientSideEntityData().clientSidePlayerEntity = playerEntity;

                // Spawning-Location
                final Location location = event.getPlayer().getLocation();
                final double origX = location.getX();
                final double origZ = location.getZ();

                // Move behind the player to make the entity not disturb players
                // Important: the negative offset!
                location.add(location.getDirection().setY(0).normalize().multiply(-(entityOffset + ThreadLocalRandom.current().nextDouble(offsetRandomizationRange))));

                final double currentXZDifference = Math.hypot(location.getX() - origX, location.getZ() - origZ);

                if (currentXZDifference < minXZDifference) {
                    final Vector moveAddVector = new Vector(-Math.sin(Math.toRadians(location.getYaw())), 0, Math.cos(Math.toRadians(location.getYaw())));
                    location.add(moveAddVector.normalize().multiply(-(minXZDifference - currentXZDifference)));
                }

                playerEntity.spawn(location);
            });
        }, 2L);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event)
    {
        // Despawn the old entity
        this.onQuit(new PlayerQuitEvent(event.getPlayer(), null));
        // Spawn another entity after the world was changed
        this.onJoin(new PlayerJoinEvent(event.getPlayer(), null));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // User not there
        if (user == null) { //dont check bypassed since it might change and it would run forever
            return;
        }
        ClientsidePlayerEntity clientSidePlayerEntity = user.getClientSideEntityData().clientSidePlayerEntity;
        if (clientSidePlayerEntity != null) {
            clientSidePlayerEntity.despawn();
        }
    }

    @Override
    public void subEnable()
    {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(AACAdditionPro.getInstance(), PacketType.Play.Client.USE_ENTITY)
        {
            @Override
            public void onPacketReceiving(final PacketEvent event)
            {
                final int entityId = event.getPacket().getIntegers().read(0);

                // Add velocity to the bot so the bot does never stand inside or in front of the player
                final User user = UserManager.getUser(event.getPlayer().getUniqueId());

                // Not bypassed
                if (user == null || user.isBypassed()) {
                    return;
                }

                final ClientsidePlayerEntity playerEntity = user.getClientSideEntityData().clientSidePlayerEntity;

                if (playerEntity != null &&
                    entityId == playerEntity.getEntityID())
                {
                    playerEntity.hurtByObserved();
                    vlManager.flag(event.getPlayer(), -1, () -> {}, () -> {});
                    event.setCancelled(true);
                }
            }
        });
    }

    @Override
    public AdditionHackType getAdditionHackType()
    {
        return AdditionHackType.KILLAURA_ENTITY;
    }
}
