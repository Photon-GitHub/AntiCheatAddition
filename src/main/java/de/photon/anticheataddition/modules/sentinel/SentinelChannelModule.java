package de.photon.anticheataddition.modules.sentinel;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.util.log.Log;
import de.photon.anticheataddition.util.pluginmessage.MessageChannel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SentinelChannelModule extends SentinelModule implements ParsedPluginMessageListener
{
    // Delay for detection to allow user initialization.
    private static final long DETECTION_DELAY_TICKS = 40L;

    private final List<String> containsAll = loadStringList(".containsAll");
    private final List<String> containsAny = loadStringList(".containsAny");

    public SentinelChannelModule(String configString)
    {
        super(configString);
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull String message)
    {
        Log.finer(() -> "SentinelChannelModule " + this.getModuleId() + " with containsAll [" + String.join(", ", containsAll)
                        + "] and containsAny [" + String.join(", ", containsAny) + "]  received message on channel " + channel
                        + " with content: " + message);

        // If containsAll or containsAny is empty, skip the respective check.
        if ((containsAll.isEmpty() || containsAll.stream().allMatch(message::contains)) &&
            (containsAny.isEmpty() || containsAny.stream().anyMatch(message::contains))) {
            Log.finer(() -> "SentinelChannelModule " + this.getModuleId() + " detected player " + player.getName() + " with detections " + this.getManagement());

            // Run the detection after a delay to allow for the user to be fully initialized.
            Bukkit.getScheduler().runTaskLater(AntiCheatAddition.getInstance(), () -> this.detection(player), DETECTION_DELAY_TICKS);
        }
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        final var builder = ModuleLoader.builder(this);
        loadStringList(".incoming_channels").stream().map(MessageChannel::of).forEach(builder::addIncomingMessageChannel);
        // No outgoing channels as the config does not allow for sending in a channel.
        return builder.build();
    }
}
