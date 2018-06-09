package de.photon.AACAdditionPro.checks.subchecks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.google.common.collect.Lists;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.api.killauraentity.KillauraEntityAddon;
import de.photon.AACAdditionPro.api.killauraentity.MovementType;
import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.fakeentity.ClientsidePlayerEntity;
import de.photon.AACAdditionPro.util.fakeentity.DelegatingKillauraEntityController;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;
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
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class KillauraEntity implements ViolationModule, Listener
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 55);

    private final Random random = new Random();

    @LoadFromConfiguration(configPath = ".on_command")
    private boolean onCommand;

    @LoadFromConfiguration(configPath = ".prefer_online_profiles")
    private boolean preferOnlineProfiles;

    private final int respawnTimer = 20 * AACAdditionPro.getInstance().getConfig().getInt(this.getConfigString() + ".respawn_timer");

    private BukkitTask respawnTask;

    @EventHandler
    public void onPlayerChatTabComplete(final PlayerChatTabCompleteEvent event)
    {
        final ClientsidePlayerEntity playerEntity = this.getClientSidePlayerEntity(event.getPlayer().getUniqueId());

        if (playerEntity != null &&
            // Online players already have a tab completion
            !preferOnlineProfiles &&
            StringUtil.startsWithIgnoreCase(playerEntity.getName(), event.getLastToken()))
        {
            event.getTabCompletions().add(playerEntity.getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerVelocity(final PlayerVelocityEvent event)
    {
        final ClientsidePlayerEntity playerEntity = this.getClientSidePlayerEntity(event.getPlayer().getUniqueId());

        if (playerEntity != null)
        {
            // Add velocity to the bot so the bot does never stand inside or in front of the player
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

        // ONLY USE THE USER -> PLAYER REFERENCE HERE TO MAKE SURE THE PLAYER IS NOT A NULLPOINTER.
        Bukkit.getScheduler().runTaskLaterAsynchronously(AACAdditionPro.getInstance(), () -> {
            // The UserManager uses a ConcurrentHashMap -> get the user async.
            final User user = UserManager.getUser(player.getUniqueId());

            // Not bypassed
            if (User.isUserInvalid(user))
            {
                return;
            }

            WrappedGameProfile gameProfile = null;
            MovementType movementType = MovementType.BASIC_FOLLOW;

            // Ask API endpoint for valid profiles
            KillauraEntityAddon killauraEntityAddon = AACAdditionPro.getInstance().getKillauraEntityAddon();
            if (killauraEntityAddon != null)
            {
                try
                {
                    gameProfile = killauraEntityAddon.getKillauraEntityGameProfile(user.getPlayer());
                    final MovementType potentialMovementType = killauraEntityAddon.getController().getMovementType();

                    if (potentialMovementType != null)
                    {
                        movementType = potentialMovementType;
                    }
                } catch (Throwable t)
                {
                    new RuntimeException("Error in plugin " + killauraEntityAddon.getPlugin().getName() + " while trying to get a killaura-entity gameprofile for " + user.getPlayer().getName(), t).printStackTrace();
                }
            }

            // No profile was set by the API
            if (gameProfile == null)
            {
                gameProfile = this.getGameProfile(user.getPlayer(), preferOnlineProfiles);
                user.getClientSideEntityData().onlineProfile = preferOnlineProfiles;

                if (gameProfile == null)
                {
                    gameProfile = this.getGameProfile(user.getPlayer(), false);
                    user.getClientSideEntityData().onlineProfile = false;

                    if (gameProfile == null)
                    {
                        VerboseSender.getInstance().sendVerboseMessage("KillauraEntity: Could not spawn entity as of too few game profiles for player " + user.getPlayer().getName(), true, true);
                        // No WrappedGameProfile can be set as there are no valid offline players.
                        return;
                    }
                }
            }

            // Make it final for the use in a lambda
            final WrappedGameProfile resultingGameProfile = gameProfile;
            final MovementType finalMovementType = movementType;

            Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> {
                // Make sure no NPE is thrown because the player logged out.
                if (User.isUserInvalid(user))
                {
                    return;
                }

                // Create the new Entity with the resultingGameProfile
                final ClientsidePlayerEntity playerEntity = new ClientsidePlayerEntity(user.getPlayer(), resultingGameProfile);

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

    private WrappedGameProfile getGameProfile(Player observedPlayer, boolean onlinePlayers)
    {
        // Use ArrayList as removal actions are unlikely.
        final List<OfflinePlayer> players = onlinePlayers ?
                                            (new ArrayList<>(Bukkit.getOnlinePlayers())) :
                                            (Lists.newArrayList(Bukkit.getOfflinePlayers()));

        OfflinePlayer chosenPlayer;
        do
        {
            // Check if we can serve OfflinePlayer profiles.
            if (players.isEmpty())
            {
                return null;
            }

            // Choose a random player
            chosenPlayer = players.remove(this.random.nextInt(players.size()));
            // and make sure it is not the observed player
        } while (chosenPlayer.getName().equals(observedPlayer.getName()));

        // Generate the GameProfile
        return new WrappedGameProfile(chosenPlayer.getUniqueId(), chosenPlayer.getName());
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
                final User user = UserManager.getUser(player.getUniqueId());
                if (User.isUserInvalid(user))
                {
                    return false;
                }
                final ClientsidePlayerEntity clientSidePlayerEntity = user.getClientSideEntityData().clientSidePlayerEntity;
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

    /**
     * Schedules a asynchronous respawn timer which activates a series of entity respawns if the entities existed for too long.
     */
    private void respawnScheduler()
    {
        // Use the wrapped one to ensure no ConcurrentModificationExceptions can appear.
        for (final User user : UserManager.getUsers())
        {
            final ClientsidePlayerEntity playerEntity = this.getClientSidePlayerEntity(user.getPlayer().getUniqueId());

            if (playerEntity != null && playerEntity.getTicksExisted() > this.respawnTimer)
            {
                this.respawnEntity(user.getPlayer());
            }
        }

        respawnTask = Bukkit.getScheduler().runTaskLaterAsynchronously(AACAdditionPro.getInstance(), this::respawnScheduler, ThreadLocalRandom.current().nextLong(respawnTimer, respawnTimer + 800));
    }

    /**
     * Gets the {@link ClientsidePlayerEntity} of an {@link User} and handles a possible respawn request.
     */
    private ClientsidePlayerEntity getClientSidePlayerEntity(final UUID uuid)
    {
        final User user = UserManager.getUser(uuid);

        // Not bypassed
        if (user == null)
        {
            return null;
        }

        if (user.isBypassed())
        {
            respawnEntity(user.getPlayer());
            return null;
        }

        return user.getClientSideEntityData().clientSidePlayerEntity;
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
