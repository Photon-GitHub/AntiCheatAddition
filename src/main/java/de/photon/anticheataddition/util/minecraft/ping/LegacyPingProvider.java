package de.photon.anticheataddition.util.minecraft.ping;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.util.reflection.ClassReflect;
import de.photon.anticheataddition.util.reflection.Reflect;
import lombok.val;
import org.bukkit.entity.Player;

import java.util.logging.Level;

final class LegacyPingProvider implements PingProvider
{
    private final ClassReflect craftPlayerReflect = Reflect.fromOBC("entity.CraftPlayer");

    public int getPing(Player player)
    {
        val craftPlayer = craftPlayerReflect.method("getHandle").invoke(player);
        try {
            return craftPlayer.getClass().getDeclaredField("ping").getInt(craftPlayer);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            AntiCheatAddition.getInstance().getLogger().log(Level.WARNING, "Failed to retrieve ping of a player: ", e);
            return 0;
        }
    }
}
