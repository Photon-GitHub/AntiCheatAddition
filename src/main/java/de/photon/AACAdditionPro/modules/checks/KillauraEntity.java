package de.photon.AACAdditionPro.modules.checks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ServerVersion;
import de.photon.AACAdditionPro.api.killauraentity.KillauraEntityAddon;
import de.photon.AACAdditionPro.api.killauraentity.Movement;
import de.photon.AACAdditionPro.modules.ListenerModule;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.RestrictedServerVersion;
import de.photon.AACAdditionPro.modules.ViolationModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.fakeentity.ClientsidePlayerEntity;
import de.photon.AACAdditionPro.util.fakeentity.DelegatingKillauraEntityController;
import de.photon.AACAdditionPro.util.fakeentity.FakeEntityUtil;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.packetwrappers.client.WrapperPlayClientUseEntity;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.StringUtil;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class KillauraEntity implements ListenerModule, ViolationModule, RestrictedServerVersion
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 55);

    @LoadFromConfiguration(configPath = ".on_command")
    private boolean onCommand;

    @LoadFromConfiguration(configPath = ".prefer_online_profiles")
    private boolean preferOnlineProfiles;

    private final int respawnTimer = 20 * AACAdditionPro.getInstance().getConfig().getInt(this.getConfigString() + ".respawn_timer");

    private BukkitTask respawnTask;

    private final Listener tabListener;

    public KillauraEntity()
    {
        // Flags the player.
        ProtocolLibrary.getProtocolManager().addPacketListener(new EntityUsePacketListener());

        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
            case MC112:
                this.tabListener = new LegacyTabListener();
                break;
            case MC113:
            case MC114:
                this.tabListener = new TabListener();
                break;
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }

    @Override
    public void enable()
    {
        AACAdditionPro.getInstance().registerListener(tabListener);
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
                if (User.isUserInvalid(user, ModuleType.KILLAURA_ENTITY)) {
                    return false;
                }
                final ClientsidePlayerEntity clientSidePlayerEntity = user.getClientSideEntityData().clientSidePlayerEntity;
                return clientSidePlayerEntity != null && clientSidePlayerEntity.isSpawned();
            }

            @Override
            public boolean setSpawnedForPlayer(Player player, boolean spawned)
            {
                final User user = UserManager.getUser(player.getUniqueId());
                if (User.isUserInvalid(user, ModuleType.KILLAURA_ENTITY)) {
                    return false;
                }

                final ClientsidePlayerEntity clientSidePlayerEntity = user.getClientSideEntityData().clientSidePlayerEntity;
                if (clientSidePlayerEntity == null) {
                    return false;
                }

                if (clientSidePlayerEntity.isSpawned()) {
                    clientSidePlayerEntity.despawn();
                } else {
                    clientSidePlayerEntity.spawn(clientSidePlayerEntity.calculateTeleportLocation());
                }
                return true;
            }

            @Override
            public boolean setSpawnedForPlayer(Player player, boolean spawned, Location spawnLocation)
            {
                final User user = UserManager.getUser(player.getUniqueId());
                if (User.isUserInvalid(user, ModuleType.KILLAURA_ENTITY)) {
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

            @Override
            public Movement getMovement()
            {
                return Movement.BASIC_FOLLOW_MOVEMENT;
            }
        });

        //Show entity for already online players on reload
        for (Player player : Bukkit.getOnlinePlayers()) {
            onJoin(new PlayerJoinEvent(player, null));
        }

        if (this.respawnTimer > 0) {
            this.respawnScheduler();
        }
    }

    @Override
    public void disable()
    {
        AACAdditionPro.getInstance().setKillauraEntityController(null);
        AACAdditionPro.getInstance().disableKillauraEntityAPI();

        // Despawn on reload
        for (User user : UserManager.getUsersUnwrapped()) {
            user.getClientSideEntityData().despawnClientSidePlayerEntity();
        }

        if (this.respawnTask != null) {
            this.respawnTask.cancel();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerVelocity(final PlayerVelocityEvent event)
    {
        final ClientsidePlayerEntity playerEntity = this.getClientSidePlayerEntity(event.getPlayer().getUniqueId());

        if (playerEntity != null) {
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

        // ONLY USE THE USER -> PLAYER REFERENCE HERE TO MAKE SURE THE PLAYER IS NOT A NULLPOINTER.
        Bukkit.getScheduler().runTaskLaterAsynchronously(AACAdditionPro.getInstance(), () -> {
            // The UserManager uses a ConcurrentHashMap -> get the user async.
            final User user = UserManager.getUser(player.getUniqueId());

            // Not bypassed
            if (User.isUserInvalid(user, this.getModuleType())) {
                return;
            }

            WrappedGameProfile gameProfile = null;
            Movement movement = Movement.BASIC_FOLLOW_MOVEMENT;

            // Ask API endpoint for valid profiles
            KillauraEntityAddon killauraEntityAddon = AACAdditionPro.getInstance().getKillauraEntityAddon();
            if (killauraEntityAddon != null) {
                try {
                    gameProfile = killauraEntityAddon.getKillauraEntityGameProfile(user.getPlayer());
                    final Movement potentialMovement = killauraEntityAddon.getController().getMovement();

                    if (potentialMovement != null) {
                        movement = potentialMovement;
                    }
                } catch (Throwable t) {
                    new RuntimeException("Error in plugin " + killauraEntityAddon.getPlugin().getName() + " while trying to get a killaura-entity gameprofile for " + user.getPlayer().getName(), t).printStackTrace();
                }
            }

            // No profile was set by the API
            boolean onlineProfile = preferOnlineProfiles;
            if (gameProfile == null) {
                gameProfile = FakeEntityUtil.getGameProfile(user.getPlayer(), preferOnlineProfiles);

                if (gameProfile == null) {
                    gameProfile = FakeEntityUtil.getGameProfile(user.getPlayer(), false);
                    onlineProfile = false;

                    if (gameProfile == null) {
                        VerboseSender.getInstance().sendVerboseMessage("KillauraEntity: Could not spawn entity as of too few game profiles for player " + user.getPlayer().getName(), true, true);
                        // No WrappedGameProfile can be set as there are no valid offline players.
                        return;
                    }
                }
            }

            // Make it final for the use in a lambda
            final WrappedGameProfile resultingGameProfile = gameProfile;
            final Movement finalMovement = movement;

            final boolean resultingOnline = onlineProfile;
            Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> {
                // Make sure no NPE is thrown because the player logged out.
                if (User.isUserInvalid(user, this.getModuleType())) {
                    return;
                }

                // Create the new Entity with the resultingGameProfile
                final ClientsidePlayerEntity playerEntity = new ClientsidePlayerEntity(user.getPlayer(), resultingGameProfile, resultingOnline);

                // Set the MovementType
                playerEntity.setCurrentMovementCalculator(finalMovement);

                // Set it as the user's active entity
                user.getClientSideEntityData().clientSidePlayerEntity = playerEntity;

                // Spawn the entity
                playerEntity.spawn(playerEntity.calculateTeleportLocation());

                if (this.onCommand) {
                    playerEntity.setVisibility(false);
                }
            });
        }, 2L);
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // User not there
        if (user == null) {
            // Don't check bypassed since it might change and it would run forever
            return;
        }

        user.getClientSideEntityData().despawnClientSidePlayerEntity();
    }

    /**
     * Schedules a asynchronous respawn timer which activates a series of entity respawns if the entities existed for too long.
     */
    private void respawnScheduler()
    {
        // Use the wrapped one to ensure no ConcurrentModificationExceptions can appear.
        for (final User user : UserManager.getUsers()) {
            final ClientsidePlayerEntity playerEntity = this.getClientSidePlayerEntity(user.getPlayer().getUniqueId());

            if (playerEntity != null && playerEntity.getTicksExisted() > this.respawnTimer) {
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
        if (user == null) {
            return null;
        }

        if (user.isBypassed(this.getModuleType())) {
            if (user.getClientSideEntityData().clientSidePlayerEntity != null) {
                user.getClientSideEntityData().despawnClientSidePlayerEntity();
            }
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

    @Override
    public Set<ServerVersion> getSupportedVersions()
    {
        return ImmutableSet.of(ServerVersion.MC188, ServerVersion.MC19, ServerVersion.MC110, ServerVersion.MC111, ServerVersion.MC112);
    }

    /**
     * Tab {@link Listener} for minecraft 1.12.2 and below.
     */
    private class LegacyTabListener implements Listener
    {
        @EventHandler
        public void onPlayerChatTabComplete(final PlayerChatTabCompleteEvent event)
        {
            final ClientsidePlayerEntity playerEntity = getClientSidePlayerEntity(event.getPlayer().getUniqueId());

            if (playerEntity != null &&
                // Online players already have a tab completion
                !preferOnlineProfiles &&
                StringUtil.startsWithIgnoreCase(playerEntity.getName(), event.getLastToken()))
            {
                event.getTabCompletions().add(playerEntity.getName());
            }
        }
    }

    /**
     * The {@link com.comphenix.protocol.events.PacketListener} that actually flags the player for hitting the entity.
     */
    private class EntityUsePacketListener extends PacketAdapter
    {
        private EntityUsePacketListener()
        {
            super(AACAdditionPro.getInstance(), PacketType.Play.Client.USE_ENTITY);
        }

        @Override
        public void onPacketReceiving(final PacketEvent event)
        {
            final WrapperPlayClientUseEntity clientUseEntityWrapper = new WrapperPlayClientUseEntity(event.getPacket());

            if (clientUseEntityWrapper.getType() == EnumWrappers.EntityUseAction.ATTACK) {
                final int entityId = clientUseEntityWrapper.getTargetID();

                // Add velocity to the bot so the bot does never stand inside or in front of the player
                final User user = UserManager.getUser(event.getPlayer().getUniqueId());

                // Not bypassed
                if (User.isUserInvalid(user, ModuleType.KILLAURA_ENTITY)) {
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
        }
    }

    /**
     * Tab {@link Listener} for minecraft 1.13 and above.
     */
    private class TabListener implements Listener
    {
        @EventHandler
        public void onTabComplete(final TabCompleteEvent event)
        {
            if (event.getSender() instanceof Player) {
                final ClientsidePlayerEntity playerEntity = getClientSidePlayerEntity(((Player) event.getSender()).getUniqueId());

                if (playerEntity != null &&
                    // Online players already have a tab completion
                    !preferOnlineProfiles &&
                    StringUtil.startsWithIgnoreCase(playerEntity.getName(), event.getBuffer()))
                {
                    event.getCompletions().add(playerEntity.getName());
                }
            }
        }
    }
}
