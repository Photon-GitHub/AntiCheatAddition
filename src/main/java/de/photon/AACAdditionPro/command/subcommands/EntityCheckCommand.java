package de.photon.AACAdditionPro.command.subcommands;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Queue;
import java.util.Set;

public class EntityCheckCommand extends InternalCommand
{
    private final boolean on_command;

    public EntityCheckCommand()
    {
        super("entitycheck", InternalPermission.ENTITYCHECK, false, (byte) 2, (byte) 2);
        on_command = AACAdditionPro.getInstance().getConfig().getBoolean("KillauraEntity.on_command");
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        final Player player = AACAdditionPro.getInstance().getServer().getPlayer(arguments.remove());

        if (player == null)
        {
            sender.sendMessage(playerNotFoundMessage);
        }
        else
        {
            final short checkDuration;
            try
            {
                checkDuration = Short.valueOf(arguments.peek());
            } catch (NumberFormatException exception)
            {
                sender.sendMessage(prefix + ChatColor.RED + "Please enter a valid duration");
                return;
            }

            if (checkDuration > 10000)
            {
                sender.sendMessage(prefix + ChatColor.RED + "The duration must at most be 10000 ticks.");
                return;
            }

            if (on_command)
            {
                final User user = UserManager.getUser(player.getUniqueId());

                if (user == null)
                {
                    sender.sendMessage(prefix + ChatColor.RED + "Invalid user parsing. Has the player logged recently?");
                    return;
                }

                if (user.isBypassed())
                {
                    sender.sendMessage(prefix + ChatColor.RED + "The target user has bypass permissions.");
                    return;
                }

                if (user.getClientSideEntityData().clientSidePlayerEntity.isVisible())
                {
                    sender.sendMessage(prefix + ChatColor.RED + "A check of the player is already in progress.");
                }
                else
                {
                    sender.sendMessage(prefix + ChatColor.GOLD + "Now checking player " + user.getPlayer().getName() + " for " + checkDuration + " ticks.");
                    VerboseSender.sendVerboseMessage("Manual entity check issued by " + sender.getName() + ": Player: " + user.getPlayer().getName() + " | Time: " + checkDuration + " ticks.");
                    user.getClientSideEntityData().clientSidePlayerEntity.setVisibility(true);

                    Bukkit.getScheduler().runTaskLater(
                            AACAdditionPro.getInstance(),
                            () -> user.getClientSideEntityData().clientSidePlayerEntity.setVisibility(false), checkDuration);
                }
            }
            else
            {
                sender.sendMessage(prefix + ChatColor.RED + "The command is disabled in the config.");
            }
        }
    }

    @Override

    protected String[] getCommandHelp()
    {
        return new String[]{
                "Run an entity-check on a player",
                "This procedure must be enabled manually in the config."
        };
    }

    @Override
    protected Set<InternalCommand> getChildCommands()
    {
        return null;
    }

    @Override
    protected String[] getTabPossibilities()
    {
        return this.getPlayerNameTabs();
    }
}
