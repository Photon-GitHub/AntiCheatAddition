package de.photon.AACAdditionPro.util.fakeentity;

import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.google.common.collect.Lists;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.reflection.Reflect;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class FakeEntityUtil
{
    private static final Field entityCountField;

    static {
        entityCountField = Reflect.fromNMS("Entity").field("entityCount").getField();
        entityCountField.setAccessible(true);
    }

    /**
     * This method tries to get a new {@link WrappedGameProfile} from the server.
     *
     * @param observedPlayer the observed player is excluded from this search as seeing himself with cause great
     *                       problems.
     * @param onlinePlayers  whether or not gameprofiles of online players should be preferred.
     */
    public static WrappedGameProfile getGameProfile(final Player observedPlayer, final boolean onlinePlayers)
    {
        // Use ArrayList as removal actions are unlikely.
        final List<OfflinePlayer> players = onlinePlayers ?
                                            (new ArrayList<>(Bukkit.getOnlinePlayers())) :
                                            (Lists.newArrayList(Bukkit.getOfflinePlayers()));

        OfflinePlayer chosenPlayer;
        do {
            // Check if we can serve OfflinePlayer profiles.
            if (players.isEmpty()) {
                return null;
            }

            // Choose a random player
            chosenPlayer = players.remove(ThreadLocalRandom.current().nextInt(players.size()));
            // and make sure it is not the observed player
        } while (chosenPlayer.getName().equals(observedPlayer.getName()));

        // Generate the GameProfile
        return new WrappedGameProfile(chosenPlayer.getUniqueId(), chosenPlayer.getName());
    }

    /**
     * This gets the next free EntityID and increases the entityCount field accordingly.
     * Prevents bypasses based on the EntityID, especially for higher numbers
     *
     * @return the next free EntityID
     */
    public static int getNextEntityID()
    {
        try {
            // Get entity id for next entity (this one)
            final int entityID = entityCountField.getInt(null);

            // Increase entity id for next entity
            entityCountField.setInt(null, entityID + 1);
            return entityID;
        } catch (IllegalAccessException e) {
            VerboseSender.getInstance().sendVerboseMessage("Unable to get a valid entity id.", true, true);
            e.printStackTrace();
            return 1000000;
        }
    }
}
