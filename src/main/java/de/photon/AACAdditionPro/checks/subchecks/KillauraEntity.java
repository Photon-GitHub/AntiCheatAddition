package de.photon.AACAdditionPro.checks.subchecks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.api.killauraentity.KillauraEntityAddon;
import de.photon.AACAdditionPro.api.killauraentity.MovementType;
import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.entities.ClientsidePlayerEntity;
import de.photon.AACAdditionPro.util.entities.DelegatingKillauraEntityController;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.storage.management.ViolationLevelManagement;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class KillauraEntity implements ViolationModule, Listener
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 55);

    @LoadFromConfiguration(configPath = ".position.entityOffset")
    private double entityOffset;

    @LoadFromConfiguration(configPath = ".position.offsetRandomizationRange")
    private double offsetRandomizationRange;

    @LoadFromConfiguration(configPath = ".position.minXZDifference")
    private double minXZDifference;

    @LoadFromConfiguration(configPath = ".on_command")
    private boolean onCommand;

    private final int respawnTimer = 20 * AACAdditionPro.getInstance().getConfig().getInt(this.getConfigString() + ".respawn_timer");

    private BukkitTask respawnTask;

    @EventHandler
    public void onPlayerChatTabComplete(final PlayerChatTabCompleteEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (user == null)
        {
            return;
        }

        if (user.isBypassed())
        {
            respawnEntity(user.getPlayer());
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
        if (user == null)
        {
            return;
        }

        if (user.isBypassed())
        {
            respawnEntity(user.getPlayer());
            return;
        }

        final ClientsidePlayerEntity playerEntity = user.getClientSideEntityData().clientSidePlayerEntity;

        if (playerEntity != null)
        {
            playerEntity.setVelocity(event.getVelocity());
        }
    }

    @EventHandler
    public void onGamemodeChange(PlayerGameModeChangeEvent event)
    {
        // The real check for the player's gamemode is located in onJoin()
        respawnEntity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent event)
    {
        final Player player = event.getPlayer();
        switch (player.getGameMode())
        {
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
        if (User.isUserInvalid(user))
        {
            return;
        }

        Bukkit.getScheduler().runTaskLaterAsynchronously(AACAdditionPro.getInstance(), () -> {
            WrappedGameProfile gameProfile = null;
            MovementType movementType = MovementType.BASIC_FOLLOW;

            // Ask API endpoint for valid profiles
            KillauraEntityAddon killauraEntityAddon = AACAdditionPro.getInstance().getKillauraEntityAddon();
            if (killauraEntityAddon != null)
            {
                try
                {
                    gameProfile = killauraEntityAddon.getKillauraEntityGameProfile(player);
                    final MovementType potentialMovementType = killauraEntityAddon.getController().getMovementType();

                    if (potentialMovementType != null)
                    {
                        movementType = potentialMovementType;
                    }
                } catch (Throwable t)
                {
                    new RuntimeException("Error in plugin " + killauraEntityAddon.getPlugin().getName() + " while trying to get a killaura-entity gameprofile for " + player.getName(), t).printStackTrace();
                }
            }

            // No profile was set by the API
            if (gameProfile == null)
            {
                // Use the offline players as a replacement
                // Encapsulate the Arrays.asList in an ArrayList to make sure removal of elements is supported.
                final List<OfflinePlayer> offlinePlayers = new ArrayList<>(Arrays.asList(Bukkit.getOfflinePlayers()));

                OfflinePlayer chosenOfflinePlayer;
                do
                {
                    // Check if we can serve OfflinePlayer profiles.
                    if (offlinePlayers.isEmpty())
                    {
                        VerboseSender.sendVerboseMessage("KillauraEntity: Could not spawn entity as of too few game profiles for player " + player.getName(), true, true);
                        // No WrappedGameProfile can be set as there are no valid offline players.
                        return;
                    }

                    // Choose a random OfflinePlayer
                    chosenOfflinePlayer = offlinePlayers.remove(ThreadLocalRandom.current().nextInt(offlinePlayers.size()));
                    // and make sure it is not the player himself
                } while (chosenOfflinePlayer.getName().equals(player.getName()));

                // Get the GameProfile
                gameProfile = new WrappedGameProfile(chosenOfflinePlayer.getUniqueId(), chosenOfflinePlayer.getName());
            }

            // Make it final for the use in a lambda
            final WrappedGameProfile resultingGameProfile = gameProfile;
            final MovementType finalMovementType = movementType;

            Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> {
                // Create the new Entity with the resultingGameProfile
                final ClientsidePlayerEntity playerEntity = new ClientsidePlayerEntity(player, resultingGameProfile, entityOffset, offsetRandomizationRange, minXZDifference);

                // Set the MovementType
                playerEntity.setMovement(finalMovementType);

                // Set it as the user's active entity
                user.getClientSideEntityData().clientSidePlayerEntity = playerEntity;

                // Spawn the entity
                playerEntity.spawn(playerEntity.calculateTeleportLocation());

                if (this.onCommand)
                {
                    playerEntity.setVisibility(false);
                }
            });
        }, 2L);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event)
    {
        respawnEntity(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event)
    {
        respawnEntity(event.getPlayer());
    }

    /**
     * This method respawns the entity as if the player had left the server and would have joined again.
     * This can also lead to despawning instead of respawning, e.g. if the player just got bypass permissions.
     *
     * @param player the player which' entity should be respawned.
     */
    private void respawnEntity(Player player)
    {
        // Wait one server tick
        Bukkit.getScheduler().runTask(
                AACAdditionPro.getInstance(),
                () ->
                {
                    // Despawn the old entity
                    this.onQuit(new PlayerQuitEvent(player, null));
                    // Spawn another entity after the world was changed
                    this.onJoin(new PlayerJoinEvent(player, null));
                });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // User not there
        if (user == null)
        {
            // Don't check bypassed since it might change and it would run forever
            return;
        }

        user.getClientSideEntityData().despawnClientSidePlayerEntity();
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
            public boolean isSpawnedFor(Player player)
            {
                User user = UserManager.getUser(player.getUniqueId());
                if (User.isUserInvalid(user))
                {
                    return false;
                }
                ClientsidePlayerEntity clientSidePlayerEntity = user.getClientSideEntityData().clientSidePlayerEntity;
                return clientSidePlayerEntity != null && clientSidePlayerEntity.isSpawned();
            }

            @Override
            public boolean setSpawnedForPlayer(Player player, boolean spawned)
            {
                final User user = UserManager.getUser(player.getUniqueId());
                if (User.isUserInvalid(user))
                {
                    return false;
                }

                final ClientsidePlayerEntity clientSidePlayerEntity = user.getClientSideEntityData().clientSidePlayerEntity;
                if (clientSidePlayerEntity == null)
                {
                    return false;
                }

                if (clientSidePlayerEntity.isSpawned())
                {
                    clientSidePlayerEntity.despawn();
                }
                else
                {
                    clientSidePlayerEntity.spawn(clientSidePlayerEntity.calculateTeleportLocation());
                }
                return true;
            }

            @Override
            public boolean setSpawnedForPlayer(Player player, boolean spawned, Location spawnLocation)
            {
                final User user = UserManager.getUser(player.getUniqueId());
                if (User.isUserInvalid(user))
                {
                    return false;
                }

                final ClientsidePlayerEntity clientSidePlayerEntity = user.getClientSideEntityData().clientSidePlayerEntity;
                if (clientSidePlayerEntity == null)
                {
                    return false;
                }

                if (clientSidePlayerEntity.isSpawned())
                {
                    clientSidePlayerEntity.despawn();
                }
                else
                {
                    //Manual location copy to prevent users from inserting locations with a bad copy method
                    Location location = new Location(spawnLocation.getWorld(), spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ(), spawnLocation.getYaw(), spawnLocation.getPitch());
                    clientSidePlayerEntity.spawn(location);
                }
                return true;
            }

            @Override
            public MovementType getMovementType()
            {
                return MovementType.BASIC_FOLLOW;
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
                if (User.isUserInvalid(user))
                {
                    return;
                }

                final ClientsidePlayerEntity playerEntity = user.getClientSideEntityData().clientSidePlayerEntity;

                if (playerEntity != null &&
                    entityId == playerEntity.getEntityID())
                {
                    playerEntity.hurtByObserved();
                    // To prevent false positives ensure the correct position.
                    playerEntity.setNeedsTeleport(true);
                    vlManager.flag(event.getPlayer(), -1, () -> {}, () -> {});
                    event.setCancelled(true);
                }
            }
        });

        //Show entity for already online players on reload
        for (Player player : Bukkit.getOnlinePlayers())
        {
            onJoin(new PlayerJoinEvent(player, null));
        }

        if (this.respawnTimer > 0)
        {
            this.respawnScheduler();
        }
    }

    /**
     * Schedules a asynchronous respawn timer which activates a series of entity respawns if the entities existed for too long.
     */
    private void respawnScheduler()
    {
        // Use the wrapped one to ensure no ConcurrentModificationExceptions can appear.
        for (final User user : UserManager.getUsers())
        {
            if (user.getClientSideEntityData().clientSidePlayerEntity.getTicksExisted() > this.respawnTimer)
            {
                this.respawnEntity(user.getPlayer());
            }
        }

        respawnTask = Bukkit.getScheduler().runTaskLaterAsynchronously(AACAdditionPro.getInstance(), this::respawnScheduler, ThreadLocalRandom.current().nextLong(300, 800));
    }

    @Override
    public void subDisable()
    {
        AACAdditionPro.getInstance().setKillauraEntityController(null);
        AACAdditionPro.getInstance().disableKillauraEntityAPI();

        // Despawn on reload
        for (User user : UserManager.getUsersUnwrapped())
        {
            user.getClientSideEntityData().despawnClientSidePlayerEntity();
        }

        if (this.respawnTask != null)
        {
            this.respawnTask.cancel();
        }
    }


    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.KILLAURA_ENTITY;
    }
}
