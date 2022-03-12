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
import de.photon.anticheataddition.user.data.TimestampKey;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import de.photon.anticheataddition.util.mathematics.Polynomial;
import de.photon.anticheataddition.util.messaging.DebugSender;
import de.photon.anticheataddition.util.minecraft.ping.PingProvider;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

public class Pingspoof extends ViolationModule implements Listener
{
    private static final Polynomial VL_CALCULATOR_BELOW_500 = new Polynomial(-1.78571E-5, 0.0723572, 1.214286);
    private static final Polynomial VL_CALCULATOR_ABOVE_500 = new Polynomial(1.372434E-10, -2.53498E-6, 0.0160475, 25.7896);

    private final int pingLeniency = loadInt(".ping_leniency", 150);
    private final int interval = loadInt(".interval", 30);

    private BukkitTask pingSpoofTask;

    public Pingspoof()
    {
        super("Pingspoof");
    }

    @Override
    public void enable()
    {
        // Seconds -> Ticks
        val tickInterval = interval * 20;

        val transactionPacket = new WrapperPlayServerTransaction();
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

                val received = user.getTimestampMap().at(TimestampKey.PINGSPOOF_RECEIVED_PACKET).getTime();
                val sent = user.getTimestampMap().at(TimestampKey.PINGSPOOF_SENT_PACKET).getTime();

                if (sent > 0) {
                    if (received <= 0) {
                        DebugSender.getInstance().sendDebug("Pingspoof-Debug: Player " + user.getPlayer().getName() + " tried to bypass pingspoof check.");
                        this.getManagement().flag(Flag.of(user).setAddedVl(35));
                    } else {
                        user.getPingspoofPing().add(MathUtil.absDiff(received, sent));
                        echoPing = PingProvider.INSTANCE.getEchoPing(user);

                        // The player has not sent the received packet.
                        difference = Math.abs(serverPing - echoPing);

                        if (difference > pingLeniency) {
                            // Make sure we do not have continuous false positives due to floating point errors.
                            user.getPingspoofPing().reloadData();

                            DebugSender.getInstance().sendDebug("Pingspoof-Debug: Player " + user.getPlayer().getName() + " tried to spoof ping. Spoofed: " + serverPing + " | Actual: " + echoPing);
                            this.getManagement().flag(Flag.of(user).setAddedVl(difference > 500 ?
                                                                               VL_CALCULATOR_ABOVE_500.apply(difference).intValue() :
                                                                               VL_CALCULATOR_BELOW_500.apply(difference).intValue()));
                        }
                    }
                }

                // Send the new packet.
                user.getTimestampMap().at(TimestampKey.PINGSPOOF_RECEIVED_PACKET).setToZero();
                transactionPacket.sendPacket(user.getPlayer());
                user.getTimestampMap().at(TimestampKey.PINGSPOOF_SENT_PACKET).update();
            }
        }, 600, tickInterval);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        val user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        // Update the received once to make sure the player is not initially flagged for not sending a received packet.
        user.getTimestampMap().at(TimestampKey.PINGSPOOF_RECEIVED_PACKET).update();
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
                           //TODO: 1.17 is not yet compatible.
                           .setAllowedServerVersions(ServerVersion.MC116.getSupVersionsTo())
                           .addPacketListeners(PacketAdapterBuilder.of(PacketType.Play.Client.TRANSACTION)
                                                                   .priority(ListenerPriority.HIGH)
                                                                   .onReceiving(event -> {
                                                                       val user = User.safeGetUserFromPacketEvent(event);
                                                                       if (User.isUserInvalid(user, this)) return;

                                                                       // We have now received the answer.
                                                                       user.getTimestampMap().at(TimestampKey.PINGSPOOF_RECEIVED_PACKET).update();
                                                                   }).build())
                           .build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).loadThresholdsToManagement().withDecay(300, 2).build();
    }
}
