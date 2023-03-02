package de.photon.anticheataddition;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import lombok.experimental.UtilityClass;
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
        final var bukkitMock = Mockito.mockStatic(Bukkit.class);
        // ProtocolLib needs this method.
        bukkitMock.when(Bukkit::getVersion).thenReturn("This server is running CraftBukkit version 3661-Spigot-19641c7-8434e36 (MC: 1.19.3) (Implementing API version 1.19.3-R0.1-SNAPSHOT)");
        bukkitMock.when(Bukkit::getBukkitVersion).thenReturn("1.19.3-R0.1-SNAPSHOT");

        final var protocolManager = Mockito.mock(ProtocolManager.class);
        final var protocolLibMock = Mockito.mockStatic(ProtocolLibrary.class);
        protocolLibMock.when(ProtocolLibrary::getProtocolManager).thenReturn(protocolManager);
    }

    /**
     * This method ensures that the mock environment is set up.
     * This only allocates the environment once, any further calls do not cause any further performance drops.
     */
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
        final var config = YamlConfiguration.loadConfiguration(new File(configPath));
        AntiCheatAddition mockAntiCheatAddition = Mockito.mock(AntiCheatAddition.class);
        Mockito.when(mockAntiCheatAddition.getConfig()).thenReturn(config);
        AntiCheatAddition.setInstance(mockAntiCheatAddition);
    }

    public static Player mockPlayer()
    {
        Player player = Mockito.mock(Player.class);
        final var uuid = UUID.randomUUID();
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
        return new User(mockPlayer());
    }

    public static ViolationModule mockViolationModule(String configString)
    {
        final var vlModule = Mockito.mock(ViolationModule.class);
        Mockito.when(vlModule.getConfigString()).thenReturn(configString);
        return vlModule;
    }
}
