package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import org.bukkit.event.Listener;

@SuppressWarnings("MethodMayBeStatic")
public class KillauraEntity implements AACAdditionProCheck, Listener
{
    // ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getAdditionHackType(), 300);

    /*@EventHandler
    public void onRespawn(final PlayerRespawnEvent event)
    {
        Bukkit.getScheduler().runTaskLater(AACAdditionPro.getInstance(), () -> {
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            // Not bypassed
            if (user == null || !user.isNotBypassed()) {
                return;
            }

            final Old_PlayerEntity playerEntity = user.getClientSideEntityData().clientSidePlayerEntity;

            if (playerEntity != null) {
                if (event.getPlayer().getGameMode() == GameMode.SURVIVAL) {
                    playerEntity.respawn();
                    playerEntity.needsToTeleport();
                } else {
                    playerEntity.despawn();
                    user.getClientSideEntityData().clientSidePlayerEntity = null;
                }
            }
        }, 20);
    }

    @EventHandler
    public void onPlayerChatTabComplete(final PlayerChatTabCompleteEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (user == null || !user.isNotBypassed()) {
            return;
        }

        final Old_PlayerEntity playerEntity = user.getClientSideEntityData().clientSidePlayerEntity;

        if (playerEntity != null) {
            if (StringUtil.startsWithIgnoreCase(playerEntity.getName(), event.getLastToken())) {
                event.getTabCompletions().add(playerEntity.getName());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(final PlayerTeleportEvent event)
    {
        // Add velocity to the bot so the bot does never stand inside or in front of the player
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (user == null || !user.isNotBypassed()) {
            return;
        }

        final Old_PlayerEntity playerEntity = user.getClientSideEntityData().clientSidePlayerEntity;

        if (playerEntity != null && playerEntity.isChecking()) {
            playerEntity.needsToTeleport();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerVelocity(final PlayerVelocityEvent event)
    {
        // Add velocity to the bot so the bot does never stand inside or in front of the player
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (user == null || !user.isNotBypassed()) {
            return;
        }

        final Old_PlayerEntity playerEntity = user.getClientSideEntityData().clientSidePlayerEntity;

        if (playerEntity != null && playerEntity.isChecking()) {
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
            if (user == null || !user.isNotBypassed()) {
                return;
            }

            //TODO: REAL NAMES AND PROFILES
            final GameProfile gameProfile = new GameProfile(UUID.randomUUID(), "BOT");

            Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> {
                final Old_PlayerEntity playerEntity = new Old_PlayerEntity(event.getPlayer(), gameProfile);
                playerEntity.spawn();
                user.getClientSideEntityData().clientSidePlayerEntity = playerEntity;
                playerEntity.setChecking(true);
                checkForHitAndReschedule(user.getPlayer());
            });
        }, 2L);
    }

    private void checkForHitAndReschedule(final Player player)
    {
        Bukkit.getScheduler().runTaskLater(AACAdditionPro.getInstance(), () -> {
            final User user = UserManager.getUser(player.getUniqueId());

            // Not bypassed
            if (user == null || !user.isNotBypassed()) {
                return;
            }

            final Old_PlayerEntity playerEntity = user.getClientSideEntityData().clientSidePlayerEntity;

            if (System.currentTimeMillis() - playerEntity.getLastHit() < 2000) {
                checkForHitAndReschedule(player);
            } else {
                playerEntity.setChecking(false);
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
            if (user == null || !user.isNotBypassed()) {
                return;
            }

            final Old_PlayerEntity playerEntity = user.getClientSideEntityData().clientSidePlayerEntity;

            if (!playerEntity.isChecking()) {
                playerEntity.setChecking(true);
                checkForHitAndReschedule(player);
            }
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
                if (user == null || !user.isNotBypassed()) {
                    return;
                }

                final Old_PlayerEntity playerEntity = user.getClientSideEntityData().clientSidePlayerEntity;

                if (playerEntity != null) {
                    if (entityId == playerEntity.getEntityId()) {
                        playerEntity.fakeHit();

                    }
                }
            }
        });
    }
*/
    @Override
    public AdditionHackType getAdditionHackType()
    {
        return AdditionHackType.KILLAURA_ENTITY;
    }
}
