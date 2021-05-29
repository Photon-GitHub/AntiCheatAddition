package de.photon.aacadditionpro.modules.checks.pingspoof;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.mathematics.Polynomial;
import de.photon.aacadditionpro.util.server.PingProvider;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class Pingspoof extends ViolationModule
{
    private static final Polynomial VL_CALCULATOR_BELOW_500 = new Polynomial(-8.92857E-6, 0.0361786, 0.607143);
    private static final Polynomial VL_CALCULATOR_ABOVE_500 = new Polynomial(6.86217E-11, -1.26749E-6, 0.00802375, 12.8948);
    private BukkitTask pingSpoofTask;
    @LoadFromConfiguration(configPath = ".ping_leniency")
    private int pingLeniency;

    public Pingspoof()
    {
        super("Pingspoof");
    }

    @Override
    public void enable()
    {
        pingSpoofTask = Bukkit.getScheduler().runTaskTimerAsynchronously(AACAdditionPro.getInstance(), () -> {
            int serverPing;
            int echoPing;
            for (Player player : Bukkit.getOnlinePlayers()) {
                serverPing = PingProvider.getPing(player);
                echoPing = PingProvider.getEchoPing(player);

                val pingDifference = (serverPing - echoPing) - pingLeniency;
                if (echoPing == PingProvider.FAIL_PING || pingDifference <= 0) continue;
                int vl;
                if (pingDifference <= 500) vl = VL_CALCULATOR_BELOW_500.apply(pingDifference).intValue();
                else if (pingDifference <= 10000) vl = VL_CALCULATOR_ABOVE_500.apply(pingDifference).intValue();
                else vl = 35;

                this.getManagement().flag(Flag.of(player).setAddedVl(vl));
            }
        }, 600, 600);
    }

    @Override
    public void disable()
    {
        pingSpoofTask.cancel();
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this).build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return new ViolationLevelManagement(this, 300L);
    }
}
