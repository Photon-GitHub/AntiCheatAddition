package de.photon.anticheataddition.commands.subcommands;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.InternalPermission;
import de.photon.anticheataddition.commands.CommandAttributes;
import de.photon.anticheataddition.commands.InternalCommand;
import de.photon.anticheataddition.commands.TabCompleteSupplier;
import de.photon.anticheataddition.protocol.packetwrappers.sentbyserver.WrapperPlayServerPlayerInfo;
import de.photon.anticheataddition.util.messaging.ChatMessage;
import de.photon.anticheataddition.util.minecraft.ping.PingProvider;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Queue;

public class TabListRemoveCommand extends InternalCommand
{
    public TabListRemoveCommand()
    {
        super("tablistremove",
              CommandAttributes.builder()
                               .minArguments(2)
                               .maxArguments(3)
                               .addCommandHelp("Removes a player from the tablist of a player and readds him after a certain time.",
                                               "Without a provided timeframe the command will add the player back to the tablist immediately.",
                                               "Syntax: /anticheataddition tablistremove <player whose tablist is affected> <player that will be removed> [<ticks>]")
                               .setPermission(InternalPermission.TABLISTREMOVE)
                               .build(),
              TabCompleteSupplier.builder().allPlayers());
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        /*
         * [0] represents the the affected player
         * [1] represents the modified player
         */
        val players = new Player[]{getPlayer(sender, arguments.remove()), getPlayer(sender, arguments.remove())};
        // Error messages are already sent by the getPlayer() method.
        for (Player player : players) if (player == null) return;

        // This prevents the crashing of the player.
        if (players[0].getUniqueId().equals(players[1].getUniqueId())) {
            ChatMessage.sendMessage(sender, "The affected player must not be the removed player.");
            return;
        }

        val ticks = arguments.isEmpty() ? Long.valueOf(0) : parseLongElseSend(arguments.poll(), sender, "Please specify a valid integer");
        if (ticks == null) return;

        sender.sendMessage(ChatColor.GOLD + "Removed player " + ChatColor.RED + players[1].getName() + ChatColor.GOLD + " from " + ChatColor.RED + players[0].getName() + ChatColor.GOLD + "'s tablist for " + ChatColor.RED + ticks + ChatColor.GOLD + " ticks.");

        updatePlayerInfo(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER, players[0], players[1]);
        if (ticks == 0) updatePlayerInfo(EnumWrappers.PlayerInfoAction.ADD_PLAYER, players[0], players[1]);
        else Bukkit.getScheduler().runTaskLater(AntiCheatAddition.getInstance(), () -> updatePlayerInfo(EnumWrappers.PlayerInfoAction.ADD_PLAYER, players[0], players[1]), ticks);
    }

    private void updatePlayerInfo(final EnumWrappers.PlayerInfoAction action, final Player affectedPlayer, final Player modifiedPlayer)
    {
        WrapperPlayServerPlayerInfo.updatePlayerInformation(action,
                                                            WrappedGameProfile.fromPlayer(modifiedPlayer),
                                                            PingProvider.INSTANCE.getPing(modifiedPlayer),
                                                            EnumWrappers.NativeGameMode.fromBukkit(modifiedPlayer.getGameMode()),
                                                            null,
                                                            affectedPlayer);
    }
}
