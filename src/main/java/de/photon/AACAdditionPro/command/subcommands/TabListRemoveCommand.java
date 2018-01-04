package de.photon.AACAdditionPro.command.subcommands;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerPlayerInfo;
import me.konsolas.aac.api.AACAPIProvider;
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
        super("tablistremove", InternalPermission.TABLISTREMOVE, false, (byte) 2, (byte) 3);
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        final Player affectedPlayer = AACAdditionPro.getInstance().getServer().getPlayer(arguments.remove());

        if (affectedPlayer == null)
        {
            sender.sendMessage(PLAYER_NOT_FOUND_MESSAGE);
        }
        else
        {
            final Player modifiedPlayer = AACAdditionPro.getInstance().getServer().getPlayer(arguments.remove());

            if (modifiedPlayer == null)
            {
                sender.sendMessage(PLAYER_NOT_FOUND_MESSAGE);
            }
            else
            {
                long ticks = arguments.isEmpty() ? 0 : Long.valueOf(arguments.remove());
                sender.sendMessage(ChatColor.GOLD + "Removed player " + ChatColor.RED + modifiedPlayer.getName() + ChatColor.GOLD + " from " + ChatColor.RED + affectedPlayer.getName() + ChatColor.GOLD + "'s tablist for " + ChatColor.RED + ticks + ChatColor.GOLD + " ticks.");

                if (ticks == 0)
                {
                    updatePlayerInfo(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER, affectedPlayer, modifiedPlayer);
                    updatePlayerInfo(EnumWrappers.PlayerInfoAction.ADD_PLAYER, affectedPlayer, modifiedPlayer);
                }
                else
                {
                    updatePlayerInfo(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER, affectedPlayer, modifiedPlayer);
                    Bukkit.getScheduler().runTaskLater(AACAdditionPro.getInstance(), () -> updatePlayerInfo(EnumWrappers.PlayerInfoAction.ADD_PLAYER, affectedPlayer, modifiedPlayer), ticks);
                }
            }
        }
    }

    private void updatePlayerInfo(final EnumWrappers.PlayerInfoAction action, final Player affectedPlayer, final Player modifiedPlayer)
    {
        // The visibility in the tablist is caused by the PlayerInformation packet.
        // Remove the player with PlayerInfo
        final PlayerInfoData playerInfoData = new PlayerInfoData(WrappedGameProfile.fromPlayer(modifiedPlayer), AACAPIProvider.getAPI().getPing(modifiedPlayer), EnumWrappers.NativeGameMode.SURVIVAL, null);

        final WrapperPlayServerPlayerInfo playerInfoWrapper = new WrapperPlayServerPlayerInfo();
        playerInfoWrapper.setAction(action);
        playerInfoWrapper.setData(Collections.singletonList(playerInfoData));

        playerInfoWrapper.sendPacket(affectedPlayer);
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
    protected String[] getTabPossibilities()
    {
        return new String[0];
    }
}
