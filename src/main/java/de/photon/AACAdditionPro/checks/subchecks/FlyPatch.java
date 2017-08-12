package de.photon.AACAdditionPro.checks.subchecks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.multiversion.ReflectionUtils;
import de.photon.AACAdditionPro.util.reflection.Reflect;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import me.konsolas.aac.api.AACAPIProvider;
import me.konsolas.aac.api.HackType;
import org.bukkit.GameMode;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FlyPatch extends PacketAdapter implements AACAdditionProCheck
{
    @LoadFromConfiguration(configPath = ".vl_increase")
    private int vl_increase;

    public FlyPatch()
    {
        super(AACAdditionPro.getInstance(), ListenerPriority.LOWEST, PacketType.Play.Client.POSITION_LOOK, PacketType.Play.Client.POSITION);
    }

    @Override
    public void onPacketReceiving(PacketEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (AACAdditionProCheck.isUserInvalid(user)) {
            return;
        }

        // Not legit flying
        if (!user.getPlayer().isFlying() &&
            // Not inside a vehicle (potential fps)
            !user.getPlayer().isInsideVehicle() &&
            // Only in survival or adventure gamemode (due to potential fps in the other gamemodes).
            (user.getPlayer().getGameMode() == GameMode.SURVIVAL || user.getPlayer().getGameMode() == GameMode.ADVENTURE))
        {
            // Get motY
            final Object nmsHandle = Reflect.from("org.bukkit.craftbukkit." + ReflectionUtils.getVersionString() + ".entity.CraftPlayer").method("getHandle").invoke(user.getPlayer());
            final double motY = Reflect.from("net.minecraft.server." + ReflectionUtils.getVersionString() + ".Entity").field("motY").from(nmsHandle).asDouble();

            if (motY != 0) {
                // Count the motion if signum got changed.
                if (user.getFlyPatchData().countNewChange(Math.signum(motY))) {
                    if (AACAPIProvider.isAPILoaded()) {
                        VerboseSender.sendVerboseMessage("Player " + user.getPlayer().getName() + " failed fly: toggled velocity too quickly.");
                        AACAPIProvider.getAPI().setViolationLevel(user.getPlayer(), HackType.FLY, AACAPIProvider.getAPI().getViolationLevel(user.getPlayer(), HackType.FLY) + vl_increase);
                    }
                }
            }
        }
    }

    @Override
    public AdditionHackType getAdditionHackType()
    {
        return AdditionHackType.FLYPATCH;
    }

}
