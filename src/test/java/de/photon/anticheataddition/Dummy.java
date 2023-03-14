package de.photon.anticheataddition;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.mockito.Mockito;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@UtilityClass
public final class Dummy
{
    private static final List<Player> mockedPlayers = new ArrayList<>();

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

    private static void ensureMockedPlayers(int amount)
    {
        while (mockedPlayers.size() < amount) {
            final var uuid = UUID.randomUUID();
            // Make sure we actually have a new UUID.
            if (mockedPlayers.stream().anyMatch(p -> p.getUniqueId().equals(uuid))) continue;

            final Player player = Mockito.mock(Player.class);
            Mockito.when(player.getName()).thenReturn("TestPlayer " + uuid.toString().substring(0, 5));
            Mockito.when(player.getUniqueId()).thenReturn(uuid);

            mockedPlayers.add(player);
        }
    }

    /**
     * Mock a {@link Player}.
     * <p>
     * Please note that there is no guarantee that two consecutive calls to this method will return two different players.
     * Use {@link #mockDistinctPlayers(int)} for that.
     */
    public static Player mockPlayer()
    {
        ensureMockedPlayers(1);
        return mockedPlayers.get(0);
    }

    /**
     * This mocks a certain amount of {@link Player}s.
     * <p>
     * The method guarantees that all returned {@link Player}s are distinct.
     */
    public static Player[] mockDistinctPlayers(int amount)
    {
        ensureMockedPlayers(amount);
        return mockedPlayers.stream().limit(amount).toArray(Player[]::new);
    }

    /**
     * Mock a {@link User}.
     * <p>
     * Please note that there is no guarantee that two consecutive calls to this method will return two different users.
     * Use {@link #mockDistinctUsers(int)} for that.
     */
    public static User mockUser()
    {
        return new User(mockPlayer());
    }

    /**
     * This mocks a certain amount of {@link User}s.
     * <p>
     * The method guarantees that all returned {@link User}s are distinct.
     */
    public static User[] mockDistinctUsers(int amount)
    {
        return Arrays.stream(mockDistinctPlayers(amount)).map(User::new).toArray(User[]::new);
    }

    public static ViolationModule mockViolationModule(String configString)
    {
        return new ViolationModule(configString)
        {
            @Override
            protected ViolationManagement createViolationManagement()
            {
                return null;
            }

            @Override
            protected ModuleLoader createModuleLoader()
            {
                return null;
            }
        };
    }
}
