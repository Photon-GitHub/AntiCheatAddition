package de.photon.aacadditionpro.command.subcommands;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.InternalPermission;
import de.photon.aacadditionpro.command.CommandAttributes;
import de.photon.aacadditionpro.command.InternalCommand;
import de.photon.aacadditionpro.command.TabCompleteSupplier;
import de.photon.aacadditionpro.util.messaging.ChatMessage;
import de.photon.aacadditionpro.util.playersimulation.displayinformation.DisplayInformation;
import de.photon.aacadditionpro.util.server.ServerUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Queue;

public class TabListRemoveCommand extends InternalCommand
{
    public TabListRemoveCommand()
    {
        super("tablistremove",
              InternalPermission.TABLISTREMOVE,
              CommandAttributes.builder()
                               .minArguments(2)
                               .maxArguments(3)
                               .setCommandHelp("Removes a player from the tablist of a player and readds him after a certain time.",
                                               "Without a provided timeframe the command will add the player back to the tablist immediately.",
                                               "Syntax: /aacadditionpro tablistremove <player whose tablist is affected> <player that will be removed> [<ticks>]")
                               .build(),
              Collections.emptySet(),
              TabCompleteSupplier.builder().allPlayers());
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        /*
         * [0] represents the the affected player
         * [1] represents the modified player
         */
        final Player[] players = new Player[2];

        for (int i = 0; i < players.length; i++) {
            players[i] = AACAdditionPro.getInstance().getServer().getPlayer(arguments.remove());

            if (players[i] == null) {
                ChatMessage.sendPlayerNotFoundMessage(sender);
                return;
            }
        }

        // This prevents the crashing of the player.
        if (players[0].getUniqueId().equals(players[1].getUniqueId())) {
            ChatMessage.sendErrorMessage(sender, "The affected player must not be the removed player.");
            return;
        }
        long ticks = 0;
        try {
            if (!arguments.isEmpty()) {
                ticks = Long.parseLong(arguments.remove());
            }
        } catch (NumberFormatException e) {
            ChatMessage.sendErrorMessage(sender, "Please specify a valid integer.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "Removed player " + ChatColor.RED + players[1].getName() + ChatColor.GOLD + " from " + ChatColor.RED + players[0].getName() + ChatColor.GOLD + "'s tablist for " + ChatColor.RED + ticks + ChatColor.GOLD + " ticks.");

        updatePlayerInfo(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER, players[0], players[1]);
        if (ticks == 0) {
            updatePlayerInfo(EnumWrappers.PlayerInfoAction.ADD_PLAYER, players[0], players[1]);
        } else {
            Bukkit.getScheduler().runTaskLater(AACAdditionPro.getInstance(), () -> updatePlayerInfo(EnumWrappers.PlayerInfoAction.ADD_PLAYER, players[0], players[1]), ticks);
        }
    }

    private void updatePlayerInfo(final EnumWrappers.PlayerInfoAction action, final Player affectedPlayer, final Player modifiedPlayer)
    {
        DisplayInformation.updatePlayerInformation(action,
                                                   WrappedGameProfile.fromPlayer(modifiedPlayer),
                                                   ServerUtil.getPing(modifiedPlayer),
                                                   EnumWrappers.NativeGameMode.fromBukkit(modifiedPlayer.getGameMode()),
                                                   null,
                                                   affectedPlayer);
    }
}
