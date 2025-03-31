package de.photon.anticheataddition;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.event.EventManager;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.mockito.Mockito;

import java.io.File;
import java.util.*;


public final class Dummy
{
    public static final String SERVER_VERSION_STRING = "This server is running CraftBukkit version 3661-Spigot-19641c7-8434e36 (MC: 1.19.4) (Implementing API version 1.19.4-R0.1-SNAPSHOT)";
    public static final String BUKKIT_VERSION_STRING = "1.19.4-R0.1-SNAPSHOT";

    private static final List<Player> mockedPlayers;
    private static final List<User> mockedUsers;
    private static final Map<String, AntiCheatAddition> mockedAntiCheatAdditionMap;

    // Mock the environment
    static {
        mockBukkit();
        mockPacketEvents();

        mockedPlayers = new ArrayList<>();
        mockedUsers = new ArrayList<>();
        mockedAntiCheatAdditionMap = new HashMap<>();
    }

    private static void mockBukkit()
    {
        final var bukkitMock = Mockito.mockStatic(Bukkit.class);
        bukkitMock.when(Bukkit::getVersion).thenReturn(SERVER_VERSION_STRING);
        bukkitMock.when(Bukkit::getBukkitVersion).thenReturn(BUKKIT_VERSION_STRING);
    }

    private static void mockPacketEvents()
    {
        final var packetEvents = Mockito.mockStatic(PacketEvents.class);
        final var packetEventsAPI = Mockito.mock(PacketEventsAPI.class);
        final var eventManager = Mockito.mock(EventManager.class);
        Mockito.when(packetEventsAPI.getEventManager()).thenReturn(eventManager);
        packetEvents.when(PacketEvents::getAPI).thenReturn(packetEventsAPI);
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
        final var acaMock = mockedAntiCheatAdditionMap.computeIfAbsent(configPath, path -> {
            final var config = YamlConfiguration.loadConfiguration(new File(path));
            final var mockAntiCheatAddition = Mockito.mock(AntiCheatAddition.class);
            Mockito.when(mockAntiCheatAddition.getConfig()).thenReturn(config);
            return mockAntiCheatAddition;
        });

        AntiCheatAddition.setInstance(acaMock);
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

    private static void ensureMockedUsers(int amount)
    {
        ensureMockedPlayers(amount);
        final int toAdd = amount - mockedUsers.size();
        for (int i = 0; i < toAdd; ++i) {
            mockedUsers.add(new User(mockedPlayers.get(amount - i - 1)));
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
        return mockedPlayers.getFirst();
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
        ensureMockedUsers(1);
        return mockedUsers.getFirst();
    }

    /**
     * This mocks a certain amount of {@link User}s.
     * <p>
     * The method guarantees that all returned {@link User}s are distinct.
     */
    public static User[] mockDistinctUsers(int amount)
    {
        ensureMockedUsers(amount);
        return mockedUsers.stream().limit(amount).toArray(User[]::new);
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
