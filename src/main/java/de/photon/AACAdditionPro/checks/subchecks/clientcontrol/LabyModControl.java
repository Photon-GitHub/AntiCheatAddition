package de.photon.AACAdditionPro.checks.subchecks.clientcontrol;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.ClientControlCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerCustomPayload;
import lombok.Getter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;

public class LabyModControl implements Listener, ClientControlCheck
{
    @Getter
    private enum LabyModFeature
    {
        // FOOD, GUI, NICK, BLOCKBUILD, CHAT, EXTRAS, ANIMATIONS, POTIONS, ARMOR, DAMAGEINDICATOR, MINIMAP_RADAR
        PLAYER_SATURATION("FOOD"),
        GUI(),
        NICK(),
        BLOCKBUILD(),
        CHAT(),
        EXTRAS(),
        OLD_ANIMATIONS("ANIMATIONS"),
        POTION_EFFECT_HUD("POTIONS"),
        ARMOUR_HUD("ARMOR"),
        DAMAGE_INDICATOR("DAMAGEINDICATOR"),
        MINIMAP_RADAR();

        private final String packetName;

        LabyModFeature()
        {
            this.packetName = this.name();
        }

        LabyModFeature(final String name)
        {
            this.packetName = name;
        }
    }

    private final HashMap<String, Boolean> featureMap = new HashMap<>();

    @EventHandler
    public void on(final PlayerJoinEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        if (user == null || user.isBypassed()) {
            return;
        }

        final WrapperPlayServerCustomPayload packetWrapper = new WrapperPlayServerCustomPayload();
        packetWrapper.setChannel("LABYMOD");

        final ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try {
            final ObjectOutputStream out = new ObjectOutputStream(byteOut);
            out.writeObject(featureMap);
        } catch (final IOException e) {
            e.printStackTrace();
        }

        packetWrapper.setContents(byteOut.toByteArray());
        packetWrapper.sendPacket(user.getPlayer());
    }

    @Override
    public List<String> getCommandsOnDetection()
    {
        return null;
    }

    @Override
    public AdditionHackType getAdditionHackType()
    {
        return AdditionHackType.LABYMOD_CONTROL;
    }

    @Override
    public void subEnable()
    {
        // Get all functions that can be disabled and put them in the HashMap
        for (final LabyModFeature feature : LabyModFeature.values()) {
            // Inversion here as true in the config means disable.
            featureMap.put(feature.getPacketName(), !AACAdditionPro.getInstance().getConfig().getBoolean(this.getAdditionHackType().getConfigString() + ".disable." + feature.name().toLowerCase()));
        }
    }
}
