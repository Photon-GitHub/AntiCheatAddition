package de.photon.aacadditionpro.util.server.ping;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.util.reflection.ClassReflect;
import de.photon.aacadditionpro.util.reflection.Reflect;
import lombok.val;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class LegacyPingProvider implements PingProvider
{
    private final ClassReflect craftPlayerReflect = Reflect.fromOBC("entity.CraftPlayer");

    public int getPing(Player player)
    {
        val craftPlayer = craftPlayerReflect.method("getHandle").invoke(player);
        try {
            return craftPlayer.getClass().getDeclaredField("ping").getInt(craftPlayer);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            AACAdditionPro.getInstance().getLogger().log(Level.WARNING, "Failed to retrieve ping of a player: ", e);
            return 0;
        }
    }
}
