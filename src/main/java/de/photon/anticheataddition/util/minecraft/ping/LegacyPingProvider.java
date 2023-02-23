package de.photon.anticheataddition.util.minecraft.ping;

import de.photon.anticheataddition.util.messaging.Log;
import de.photon.anticheataddition.util.reflection.ClassReflect;
import de.photon.anticheataddition.util.reflection.Reflect;
import org.bukkit.entity.Player;

final class LegacyPingProvider implements PingProvider
{
    private final ClassReflect craftPlayerReflect = Reflect.fromOBC("entity.CraftPlayer");

    public int getPing(Player player)
    {
        final var craftPlayer = craftPlayerReflect.method("getHandle").invoke(player);
        try {
            return craftPlayer.getClass().getDeclaredField("ping").getInt(craftPlayer);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            Log.error("Failed to retrieve ping of a player: ", e);
            return 0;
        }
    }
}
