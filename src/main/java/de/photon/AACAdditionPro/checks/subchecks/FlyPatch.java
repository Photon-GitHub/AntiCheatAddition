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
import me.konsolas.aac.api.AACAPIProvider;
import me.konsolas.aac.api.HackType;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FlyPatch extends PacketAdapter implements AACAdditionProCheck
{
    @LoadFromConfiguration(configPath = ".vl_increase")
    private int vl_increase;

    private Field motYField;
    private Method getHandle;

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

        try {
            // Get motY
            final Object nmsHandle = this.getHandle.invoke(user.getPlayer());
            final double motY = this.motYField.getDouble(nmsHandle);

            if (motY != 0) {
                // Count the motion if signum got changed.
                if (user.getFlyPatchData().countNewChange(Math.signum(motY))) {
                    if (AACAPIProvider.isAPILoaded()) {
                        AACAPIProvider.getAPI().setViolationLevel(user.getPlayer(), HackType.FLY, AACAPIProvider.getAPI().getViolationLevel(user.getPlayer(), HackType.FLY) + vl_increase);
                    }
                }
            }

        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public AdditionHackType getAdditionHackType()
    {
        return AdditionHackType.FLYPATCH;
    }

    @Override
    public void subEnable()
    {
        // Reflection stuff
        try {
            //Get the String representing the version, e.g. v1_11_R1
            final String version = ReflectionUtils.getVersionString();

            // Load version class and reflect getHandle() out of it
            this.getHandle = ReflectionUtils.getMethodFromPath("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer", "getHandle", true);

            // Get the motX and motZ fields
            final Class entityPlayerClazz = ReflectionUtils.loadClassFromPath("net.minecraft.server." + version + ".Entity");

            //Fields
            this.motYField = entityPlayerClazz.getDeclaredField("motY");
            this.motYField.setAccessible(true);
        } catch (ArrayIndexOutOfBoundsException | NoSuchMethodException | ClassNotFoundException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
}
