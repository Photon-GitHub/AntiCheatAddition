package de.photon.AACAdditionPro.checks.subchecks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.entities.ClientsidePlayerEntity;
import de.photon.AACAdditionPro.util.storage.management.ViolationLevelManagement;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.StringUtil;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class KillauraEntity implements AACAdditionProCheck, Listener
{
    ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getAdditionHackType(), 300);

    @EventHandler
    public void onPlayerChatTabComplete(final PlayerChatTabCompleteEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (user == null || user.isBypassed()) {
            return;
        }

        final ClientsidePlayerEntity playerEntity = user.getClientSideEntityData().clientSidePlayerEntity;

        if (playerEntity != null) {
            if (StringUtil.startsWithIgnoreCase(playerEntity.getName(), event.getLastToken())) {
                event.getTabCompletions().add(playerEntity.getName());
            }
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
            if (event.getPlayer().getGameMode() != GameMode.SURVIVAL) {
                return;
            }

            // Add velocity to the bot so the bot does never stand inside or in front of the player
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            // Not bypassed
            if (user == null || user.isBypassed()) {
                return;
            }

            //TODO: REAL NAMES AND PROFILES
            final WrappedGameProfile gameProfile = new WrappedGameProfile(UUID.randomUUID(), "BOT");

            Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> {
                final ClientsidePlayerEntity playerEntity = new ClientsidePlayerEntity(event.getPlayer(), gameProfile);
                user.getClientSideEntityData().clientSidePlayerEntity = playerEntity;

                // Spawn-Location

                Vector spawnOffset = user.getPlayer().getLocation().getDirection().clone().multiply(-5);

                Location spawnLocation = user.getPlayer().getLocation();
                spawnLocation.add(spawnOffset);
                spawnLocation.add(ThreadLocalRandom.current().nextDouble(2D), ThreadLocalRandom.current().nextDouble(2D), ThreadLocalRandom.current().nextDouble(2D));

                playerEntity.spawn(spawnLocation);

                checkForHitAndReschedule(user.getPlayer());
            });
        }, 2L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // User not there
        if (user == null) { //dont check bypassed since it might change and it would run forever
            System.out.println("no user there");
            return;
        }
        ClientsidePlayerEntity clientSidePlayerEntity = user.getClientSideEntityData().clientSidePlayerEntity;
        if (clientSidePlayerEntity != null) {
            clientSidePlayerEntity.despawn();
        }
    }

    private void checkForHitAndReschedule(final Player player)
    {
        Bukkit.getScheduler().runTaskLater(AACAdditionPro.getInstance(), () -> {
            final User user = UserManager.getUser(player.getUniqueId());

            // Not bypassed
            if (user == null || user.isBypassed()) {
                return;
            }

            final ClientsidePlayerEntity playerEntity = user.getClientSideEntityData().clientSidePlayerEntity;

            if (System.currentTimeMillis() - playerEntity.lastHurtMillis < 2000) {
                checkForHitAndReschedule(player);
            }
        }, 5 + ThreadLocalRandom.current().nextInt(35));
    }

    @EventHandler
    public void onEntityDamage(final EntityDamageByEntityEvent event)
    {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            final Player player = (Player) event.getDamager();
            final User user = UserManager.getUser(player.getUniqueId());

            // Not bypassed
            if (user == null || user.isBypassed()) {
                return;
            }

            vlManager.flag(player, -1, () -> {}, () -> {});
            checkForHitAndReschedule(player);
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

                if (playerEntity != null) {
                    if (entityId == playerEntity.getEntityID()) {
                        playerEntity.hurtByObserved();
                        event.setCancelled(true);
                    }
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
