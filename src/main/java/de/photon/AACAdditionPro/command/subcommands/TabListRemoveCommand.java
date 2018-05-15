package de.photon.AACAdditionPro.command.subcommands;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.util.fakeentity.displayinformation.DisplayInformation;
import me.konsolas.aac.api.AACAPIProvider;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Queue;

public class TabListRemoveCommand extends InternalCommand
{
    public TabListRemoveCommand()
    {
        super("tablistremove", InternalPermission.TABLISTREMOVE, false, (byte) 2, (byte) 3);
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        /*
         * [0] represents the the affected player
         * [1] represents the modified player
         */
        final Player[] players = new Player[2];

        for (int i = 0; i < players.length; i++)
        {
            players[i] = AACAdditionPro.getInstance().getServer().getPlayer(arguments.remove());

            if (players[i] == null)
            {
                InternalCommand.sendPlayerNotFoundMessage(sender);
                return;
            }
        }

        // This prevents the crashing of the player.
        if (players[0].getUniqueId().equals(players[1].getUniqueId()))
        {
            sender.sendMessage(PREFIX + ChatColor.RED + "The affected player must not be the removed player.");
            return;
        }

        long ticks = arguments.isEmpty() ? 0 : Long.valueOf(arguments.remove());
        sender.sendMessage(ChatColor.GOLD + "Removed player " + ChatColor.RED + players[1].getName() + ChatColor.GOLD + " from " + ChatColor.RED + players[0].getName() + ChatColor.GOLD + "'s tablist for " + ChatColor.RED + ticks + ChatColor.GOLD + " ticks.");

        updatePlayerInfo(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER, players[0], players[1]);
        if (ticks == 0)
        {
            updatePlayerInfo(EnumWrappers.PlayerInfoAction.ADD_PLAYER, players[0], players[1]);
        }
        else
        {
            Bukkit.getScheduler().runTaskLater(AACAdditionPro.getInstance(), () -> updatePlayerInfo(EnumWrappers.PlayerInfoAction.ADD_PLAYER, players[0], players[1]), ticks);
        }
    }

    private void updatePlayerInfo(final EnumWrappers.PlayerInfoAction action, final Player affectedPlayer, final Player modifiedPlayer)
    {
        DisplayInformation.updatePlayerInformation(action,
                                                   WrappedGameProfile.fromPlayer(modifiedPlayer),
                                                   AACAPIProvider.getAPI().getPing(modifiedPlayer),
                                                   EnumWrappers.NativeGameMode.fromBukkit(modifiedPlayer.getGameMode()),
                                                   null,
                                                   affectedPlayer);
    }

    @Override
    protected String[] getCommandHelp()
    {
        return new String[]{
                "Removes a player from the tablist of a player and readds him after a certain time.",
                "Without a provided timeframe the command will add the player back to the tablist immediately.",
                "Syntax: /aacadditionpro tablistremove <player whose tablist is affected> <player that will be removed> [<ticks>]"
        };
    }

    @Override
    protected List<String> getTabPossibilities()
    {
        return this.getPlayerNameTabs();
    }
}
