package de.photon.aacadditionproold.modules.clientcontrol;

import com.google.common.collect.ImmutableMap;
import de.photon.aacadditionproold.AACAdditionPro;
import de.photon.aacadditionproold.ServerVersion;
import de.photon.aacadditionproold.modules.ListenerModule;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.modules.RestrictedServerVersion;
import de.photon.aacadditionproold.user.User;
import de.photon.aacadditionproold.user.UserManager;
import de.photon.aacadditionproold.util.packetwrappers.server.WrapperPlayServerCustomPayload;
import de.photon.aacadditionproold.util.pluginmessage.MessageChannel;
import lombok.Getter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class OldLabyModControl extends ClientControlModule implements ListenerModule, RestrictedServerVersion
{
    @Getter
    private static final OldLabyModControl instance = new OldLabyModControl();

    private Map<String, Boolean> featureMap;

    @Override
    public void enable()
    {
        final ImmutableMap.Builder<String, Boolean> featureMapBuilder = ImmutableMap.builder();
        featureMap = featureMapBuilder.put("FOOD", !AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.player_saturation"))
                                      .put("GUI", !AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.gui"))
                                      .put("NICK", !AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.nick"))
                                      .put("BLOCKBUILD", !AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.blockbuild"))
                                      .put("ANIMATIONS", !AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.old_animations"))
                                      .put("POTIONS", !AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.potion_effect_hud"))
                                      .put("ARMOR", !AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.armour_hud"))
                                      .put("DAMAGEINDICATOR", !AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.damage_indicator"))
                                      .put("MINIMAP_RADAR", !AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.minimap_radar")).build();
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        final WrapperPlayServerCustomPayload packetWrapper = new WrapperPlayServerCustomPayload();
        packetWrapper.setChannel(new MessageChannel("minecraft", "labymod", "LABYMOD"));

        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(byteOut))
        {
            out.writeObject(featureMap);
            packetWrapper.setContents(byteOut.toByteArray());
            packetWrapper.sendPacket(user.getPlayer());
        } catch (final IOException e) {
            AACAdditionPro.getInstance().getLogger().log(Level.SEVERE, "OldLabyModControl failed to send feature map.", e);
        }
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.OLD_LABYMOD_CONTROL;
    }

    @Override
    public Set<ServerVersion> getSupportedVersions()
    {
        return ServerVersion.LEGACY_PLUGIN_MESSAGE_VERSIONS;
    }
}
