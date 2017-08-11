package de.photon.AACAdditionPro.checks.subchecks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.api.KillauraEntityAddon;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.entities.ClientsidePlayerEntity;
import de.photon.AACAdditionPro.util.entities.DelegatingKillauraEntityController;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.storage.management.ViolationLevelManagement;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
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
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getAdditionHackType(), 300);

    @LoadFromConfiguration(configPath = ".position.entityOffset")
    private double entityOffset;

    @LoadFromConfiguration(configPath = ".position.offsetRandomizationRange")
    private double offsetRandomizationRange;

    @LoadFromConfiguration(configPath = ".position.minXZDifference")
    private double minXZDifference;

    private boolean spawnAtJoin;

    @EventHandler
    public void onPlayerChatTabComplete(final PlayerChatTabCompleteEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (AACAdditionProCheck.isUserInvalid(user)) {
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
        if (AACAdditionProCheck.isUserInvalid(user)) {
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
            final Player player = event.getPlayer();
            switch (player.getGameMode()) {
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
            final User user = UserManager.getUser(player.getUniqueId());

            // Not bypassed
            if (AACAdditionProCheck.isUserInvalid(user)) {
                return;
            }

            WrappedGameProfile gameProfile_ = null;

            // Ask API endpoint for valid profiles
            KillauraEntityAddon killauraEntityAddon = AACAdditionPro.getInstance().getKillauraEntityAddon();
            if (killauraEntityAddon != null) {
                try {
                    gameProfile_ = killauraEntityAddon.getKillauraEntityGameProfile(player);
                } catch (Throwable t) {
                    new RuntimeException("Error in plugin " + killauraEntityAddon.getPlugin().getName() + " while trying to get a killaura-entity gameprofile for " + player.getName(), t).printStackTrace();
                }
            }

            OfflinePlayer[] offlinePlayers = Bukkit.getOfflinePlayers();
            // If the API endpoint can't serve valid profiles, check if we can serve OfflinePlayer profiles.
            if (gameProfile_ == null && offlinePlayers.length < 1) {
                return;
            }

            if (gameProfile_ == null) {
                final OfflinePlayer chosenOfflinePlayer = offlinePlayers[ThreadLocalRandom.current().nextInt(offlinePlayers.length)];
                gameProfile_ = new WrappedGameProfile(chosenOfflinePlayer.getUniqueId(), chosenOfflinePlayer.getName());
            }

            final WrappedGameProfile gameProfile = gameProfile_;

            Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> {
                final ClientsidePlayerEntity playerEntity = new ClientsidePlayerEntity(player, gameProfile, entityOffset, offsetRandomizationRange, minXZDifference);
                user.getClientSideEntityData().clientSidePlayerEntity = playerEntity;
                if (spawnAtJoin) {
                    final Location location = calculateLocationBehindPlayer(player, entityOffset, offsetRandomizationRange, minXZDifference);
                    playerEntity.spawn(location);
                }
            });
        }, 2L);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event)
    {
        // Wait one server tick
        Bukkit.getScheduler().runTask(
                AACAdditionPro.getInstance(),
                () ->
                {
                    // Despawn the old entity
                    this.onQuit(new PlayerQuitEvent(event.getPlayer(), null));
                    // Spawn another entity after the world was changed
                    this.onJoin(new PlayerJoinEvent(event.getPlayer(), null));
                });
    }

    public static Location calculateLocationBehindPlayer(Player player, double entityOffset, double offsetRandomizationRange, double minXZDifference)
    {
        // Spawning-Location
        final Location location = player.getLocation();
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
        return location;
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
        AACAdditionPro.getInstance().setKillauraEntityController(new DelegatingKillauraEntityController(null) //extending the delegation for obfuscation purposes, does not make any difference at the end
        {
            @Override
            public boolean isValid()
            {
                return true;
            }

            @Override
            public boolean isSpawnAtJoin()
            {
                return spawnAtJoin;
            }

            @Override
            public void setSpawnAtJoin(boolean spawnAtJoin)
            {
                KillauraEntity.this.spawnAtJoin = spawnAtJoin;
            }

            @Override
            public boolean isSpawnedFor(Player player)
            {
                User user = UserManager.getUser(player.getUniqueId());
                if (AACAdditionProCheck.isUserInvalid(user)) {
                    return false;
                }
                ClientsidePlayerEntity clientSidePlayerEntity = user.getClientSideEntityData().clientSidePlayerEntity;
                return clientSidePlayerEntity != null && clientSidePlayerEntity.isSpawned();
            }

            @Override
            public boolean setSpawnedForPlayer(Player player, boolean spawned)
            {
                final User user = UserManager.getUser(player.getUniqueId());
                if (AACAdditionProCheck.isUserInvalid(user)) {
                    return false;
                }

                final ClientsidePlayerEntity clientSidePlayerEntity = user.getClientSideEntityData().clientSidePlayerEntity;
                if (clientSidePlayerEntity == null) {
                    return false;
                }

                if (clientSidePlayerEntity.isSpawned()) {
                    clientSidePlayerEntity.despawn();
                } else {
                    clientSidePlayerEntity.spawn(calculateLocationBehindPlayer(player, KillauraEntity.this.entityOffset, KillauraEntity.this.offsetRandomizationRange, KillauraEntity.this.minXZDifference));
                }
                return true;
            }

            @Override
            public boolean setSpawnedForPlayer(Player player, boolean spawned, Location spawnLocation)
            {
                final User user = UserManager.getUser(player.getUniqueId());
                if (AACAdditionProCheck.isUserInvalid(user)) {
                    return false;
                }

                final ClientsidePlayerEntity clientSidePlayerEntity = user.getClientSideEntityData().clientSidePlayerEntity;
                if (clientSidePlayerEntity == null) {
                    return false;
                }

                if (clientSidePlayerEntity.isSpawned()) {
                    clientSidePlayerEntity.despawn();
                } else {
                    //Manual location copy to prevent users from inserting locations with a bad copy method
                    Location location = new Location(spawnLocation.getWorld(), spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ(), spawnLocation.getYaw(), spawnLocation.getPitch());
                    clientSidePlayerEntity.spawn(location);
                }
                return true;
            }
        });

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(AACAdditionPro.getInstance(), PacketType.Play.Client.USE_ENTITY)
        {
            @Override
            public void onPacketReceiving(final PacketEvent event)
            {
                final int entityId = event.getPacket().getIntegers().read(0);

                // Add velocity to the bot so the bot does never stand inside or in front of the player
                final User user = UserManager.getUser(event.getPlayer().getUniqueId());

                // Not bypassed
                if (AACAdditionProCheck.isUserInvalid(user)) {
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

        //Show entity for already online players on reload
        for (Player player : Bukkit.getOnlinePlayers()) {
            onJoin(new PlayerJoinEvent(player, null));
        }
    }

    @Override
    public void subDisable()
    {
        AACAdditionPro.getInstance().setKillauraEntityController(null);
        AACAdditionPro.getInstance().disableKillauraEntityAPI();

        // Despawn on reload
        for (User user : UserManager.getUsers()) {

            final ClientsidePlayerEntity clientSidePlayerEntity = user.getClientSideEntityData().clientSidePlayerEntity;

            if (clientSidePlayerEntity != null) {
                clientSidePlayerEntity.despawn();
            }
        }
    }


    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public AdditionHackType getAdditionHackType()
    {
        return AdditionHackType.KILLAURA_ENTITY;
    }
}
