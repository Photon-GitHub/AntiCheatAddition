package de.photon.anticheataddition.modules.checks.pingspoof;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.protocol.packetwrappers.sentbyserver.WrapperPlayServerTransaction;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import de.photon.anticheataddition.util.mathematics.Polynomial;
import de.photon.anticheataddition.util.mathematics.TimeUtil;
import de.photon.anticheataddition.util.minecraft.ping.PingProvider;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("deprecation")
public final class Pingspoof extends ViolationModule implements Listener
{
    public static final Pingspoof INSTANCE = new Pingspoof();

    private static final Polynomial VL_CALCULATOR_BELOW_500 = new Polynomial(-1.78571E-5, 0.0723572, 1.214286);
    private static final Polynomial VL_CALCULATOR_ABOVE_500 = new Polynomial(1.372434E-10, -2.53498E-6, 0.0160475, 25.7896);

    private final int pingLeniency = loadInt(".ping_leniency", 150);

    private BukkitTask pingSpoofTask;

    private Pingspoof()
    {
        super("Pingspoof");
    }

    @Override
    public void enable()
    {
        // Seconds -> Ticks
        final long tickInterval = TimeUtil.toTicks(TimeUnit.SECONDS, loadInt(".interval", 30));

        final var transactionPacket = new WrapperPlayServerTransaction();
        transactionPacket.setAccepted(false);
        transactionPacket.setAccepted(false);
        transactionPacket.setActionNumber((short) 0);
        transactionPacket.setWindowId(0);


        pingSpoofTask = Bukkit.getScheduler().runTaskTimerAsynchronously(AntiCheatAddition.getInstance(), () -> {
            long serverPing;
            long echoPing;
            long difference;

            for (User user : User.getUsersUnwrapped()) {
                if (User.isUserInvalid(user, this)) continue;

                serverPing = PingProvider.INSTANCE.getPing(user.getPlayer());

                final long received = user.getTimeMap().at(TimeKey.PINGSPOOF_RECEIVED_PACKET).getTime();
                final long sent = user.getTimeMap().at(TimeKey.PINGSPOOF_SENT_PACKET).getTime();

                if (sent > 0) {
                    if (received <= 0) {
                        this.getManagement().flag(Flag.of(user)
                                                      .setAddedVl(35)
                                                      .setDebug(() -> "Pingspoof-Debug: Player " + user.getPlayer().getName() + " tried to bypass pingspoof check."));
                    } else {
                        user.getPingspoofPing().add(MathUtil.absDiff(received, sent));
                        echoPing = PingProvider.INSTANCE.getEchoPing(user);

                        // The player has not sent the received packet.
                        difference = MathUtil.absDiff(serverPing, echoPing);

                        if (difference > pingLeniency) {
                            // Make sure we do not have continuous false positives due to floating point errors.
                            user.getPingspoofPing().reloadData();

                            final long finalServerPing = serverPing;
                            final long finalEchoPing = echoPing;
                            this.getManagement().flag(Flag.of(user).setAddedVl(difference > 500 ?
                                                                               VL_CALCULATOR_ABOVE_500.apply(difference).intValue() :
                                                                               VL_CALCULATOR_BELOW_500.apply(difference).intValue())
                                                          .setDebug(() -> "Pingspoof-Debug: Player " + user.getPlayer().getName() + " tried to spoof ping. Spoofed: " + finalServerPing + " | Actual: " + finalEchoPing));
                        }
                    }
                }

                // Send the new packet.
                user.getTimeMap().at(TimeKey.PINGSPOOF_RECEIVED_PACKET).setToZero();
                transactionPacket.sendPacket(user.getPlayer());
                user.getTimeMap().at(TimeKey.PINGSPOOF_SENT_PACKET).update();
            }
        }, 600, tickInterval);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        final var user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        // Update the received once to make sure the player is not initially flagged for not sending a received packet.
        user.getTimeMap().at(TimeKey.PINGSPOOF_RECEIVED_PACKET).update();
    }

    @Override
    public void disable()
    {
        pingSpoofTask.cancel();
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .setAllowedServerVersions(ServerVersion.MC116.getSupVersionsTo())
                           .addPacketListeners(PacketAdapterBuilder.of(this, PacketType.Play.Client.TRANSACTION)
                                                                   .priority(ListenerPriority.HIGH)
                                                                   // We have now received the answer.
                                                                   .onReceiving((event, user) -> user.getTimeMap().at(TimeKey.PINGSPOOF_RECEIVED_PACKET).update())
                                                                   .build())
                           .build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).loadThresholdsToManagement().withDecay(300, 2).build();
    }
}
