package de.photon.anticheataddition;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.mockito.Mockito;

import java.io.File;
import java.util.UUID;

@UtilityClass
public final class Dummy
{
    // Mock the environment.
    static {
        val bukkitMock = Mockito.mockStatic(Bukkit.class);
        bukkitMock.when(Bukkit::getVersion).thenReturn("This server is running CraftBukkit version 3467-Spigot-ffceeae-e6cc7c7 (MC: 1.18.2) (Implementing API version 1.18.2-R0.1-SNAPSHOT)");

        val protocolManager = Mockito.mock(ProtocolManager.class);
        val protocolLibMock = Mockito.mockStatic(ProtocolLibrary.class);
        protocolLibMock.when(ProtocolLibrary::getProtocolManager).thenReturn(protocolManager);
    }

    public static void mockEnvironment()
    {
        // Do nothing, this is already done via the static constructor.
    }

    public static void mockAntiCheatAddition()
    {
        mockAntiCheatAddition("src/main/resources/config.yml");
    }

    public static void mockAntiCheatAddition(String configPath)
    {
        val config = YamlConfiguration.loadConfiguration(new File(configPath));
        AntiCheatAddition mockAntiCheatAddition = Mockito.mock(AntiCheatAddition.class);
        Mockito.when(mockAntiCheatAddition.getConfig()).thenReturn(config);
        AntiCheatAddition.setInstance(mockAntiCheatAddition);
    }

    public static Player mockPlayer()
    {
        Player player = Mockito.mock(Player.class);
        val uuid = UUID.randomUUID();
        val name = "TestPlayer" + Long.toHexString(uuid.getLeastSignificantBits());
        Mockito.when(player.getName()).thenReturn("TestPlayer");
        Mockito.when(player.getUniqueId()).thenReturn(uuid);
        return player;
    }

    /**
     * Mock User.
     * Make sure to call mockEnvironment beforehand.
     */
    public static User mockUser()
    {
        return User.createFromPlayer(mockPlayer());
    }

    public static ViolationModule mockViolationModule(String configString)
    {
        val vlModule = Mockito.mock(ViolationModule.class);
        Mockito.when(vlModule.getConfigString()).thenReturn(configString);
        return vlModule;
    }
}
