package de.photon.AACAdditionPro.command.subcommands;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Queue;
import java.util.Set;

public class EntityCheckCommand extends InternalCommand
{
    public EntityCheckCommand()
    {
        super("entitycheck", InternalPermission.ENTITYCHECK, false, (byte) 1, (byte) 2);
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        final User user = UserManager.getUser(AACAdditionPro.getInstance().getServer().getPlayer(arguments.remove()).getUniqueId());

        if (user == null)
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

            if (AACAdditionPro.getInstance().getConfig().getBoolean("KillauraEntity.on_command"))
            {
                if (user.getClientSideEntityData().clientSidePlayerEntity.isVisible())
                {
                    sender.sendMessage(prefix + ChatColor.RED + "A check of the player is already in progress.");
                }
                else
                {
                    sender.sendMessage(prefix + ChatColor.GOLD + "Now checking player " + user.getPlayer().getName() + " for " + checkDuration + " ticks.");
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
