package de.photon.aacadditionpro;

import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.DataMap;
import de.photon.aacadditionpro.user.data.TimestampMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.mockito.Mockito;

import java.io.File;

public class Dummy
{
    public static AACAdditionPro mockAACAdditionPro()
    {
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"));
        AACAdditionPro mockAACAdditionPro = Mockito.mock(AACAdditionPro.class);
        Mockito.when(mockAACAdditionPro.getConfig()).thenReturn(config);
        AACAdditionPro.setInstance(mockAACAdditionPro);
        return mockAACAdditionPro;
    }

    public static Player mockPlayer()
    {
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getName()).thenReturn("TestPlayer");
        return player;
    }

    public static User mockUser()
    {
        User user = Mockito.mock(User.class);
        DataMap dataMap = new DataMap();
        TimestampMap timestampMap = new TimestampMap();

        Mockito.when(user.getDataMap()).thenReturn(dataMap);
        Mockito.when(user.getTimestampMap()).thenReturn(timestampMap);
        return user;
    }

    public static ViolationModule mockViolationModule()
    {
        return Mockito.mock(ViolationModule.class);
    }
}
