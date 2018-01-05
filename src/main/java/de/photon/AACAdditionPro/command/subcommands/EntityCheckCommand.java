package de.photon.AACAdditionPro.command.subcommands;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.Module;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.subchecks.KillauraEntity;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Queue;

public class EntityCheckCommand extends InternalCommand
{
    // If KillauraEntity is not enabled the ModuleManager will throw an error.
    private final boolean commandActive = AACAdditionPro.getInstance().getConfig().getBoolean("KillauraEntity.commandActive") &&
                                          // The on_command mode must be enabled, otherwise this command is useless.
                                          AACAdditionPro.getInstance().getConfig().getBoolean("KillauraEntity.on_command");

    private static final byte MAX_ITERATIONS = 2;

    public EntityCheckCommand()
    {
        super("entitycheck", InternalPermission.ENTITYCHECK, false, (byte) 2, (byte) 2);
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        if (commandActive)
        {
            final Player player = AACAdditionPro.getInstance().getServer().getPlayer(arguments.remove());

            if (player == null)
            {
                sender.sendMessage(PLAYER_NOT_FOUND_MESSAGE);
            }
            else
            {
                final short checkDuration;
                try
                {
                    checkDuration = Short.valueOf(arguments.remove());
                } catch (NumberFormatException exception)
                {
                    sender.sendMessage(PREFIX + ChatColor.RED + "Please enter a valid duration");
                    return;
                }

                if (checkDuration > 10000)
                {
                    sender.sendMessage(PREFIX + ChatColor.RED + "The duration must at most be 10000 ticks.");
                    return;
                }

                final User user = UserManager.getUser(player.getUniqueId());

                if (user == null)
                {
                    sender.sendMessage(PREFIX + ChatColor.RED + "Invalid user parsing. Has the player logged recently?");
                    return;
                }

                if (user.isBypassed())
                {
                    sender.sendMessage(PREFIX + ChatColor.RED + "The target user has bypass permissions.");
                    return;
                }

                if (user.getClientSideEntityData().clientSidePlayerEntity == null)
                {
                    try
                    {
                        final Module module = AACAdditionPro.getInstance().getModuleManager().getModule(ModuleType.KILLAURA_ENTITY);

                        final Method respawnEntityMethod = KillauraEntity.class.getDeclaredMethod("respawnEntity", Player.class);
                        respawnEntityMethod.setAccessible(true);
                        respawnEntityMethod.invoke(module, user.getPlayer());

                        arguments.add(player.getName());
                        arguments.add(String.valueOf(checkDuration));

                        if (user.getClientSideEntityData().respawnTrys++ > MAX_ITERATIONS)
                        {
                            throw new IllegalStateException("Too many respawn iterations.");
                        }

                        Bukkit.getScheduler().runTaskLater(AACAdditionPro.getInstance(), () -> this.execute(sender, arguments), 5L);
                    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | IllegalStateException e)
                    {
                        sender.sendMessage(PREFIX + ChatColor.RED + "Critical error whilst trying to respawn the entity.");
                        VerboseSender.sendVerboseMessage("Critical error whilst trying to respawn the entity.", true, true);
                        e.printStackTrace();
                    }
                }
                else
                {
                    user.getClientSideEntityData().respawnTrys = 0;
                    if (user.getClientSideEntityData().clientSidePlayerEntity.isVisible())
                    {
                        sender.sendMessage(PREFIX + ChatColor.RED + "A check of the player is already in progress.");
                    }
                    else
                    {
                        sender.sendMessage(PREFIX + ChatColor.GOLD + "Now checking player " + user.getPlayer().getName() + " for " + checkDuration + " ticks.");
                        VerboseSender.sendVerboseMessage("Manual entity check issued by " + sender.getName() + ": Player: " + user.getPlayer().getName() + " | Time: " + checkDuration + " ticks.");
                        user.getClientSideEntityData().clientSidePlayerEntity.setVisibility(true);

                        Bukkit.getScheduler().runTaskLater(
                                AACAdditionPro.getInstance(),
                                () -> user.getClientSideEntityData().clientSidePlayerEntity.setVisibility(false), checkDuration);
                    }
                }
            }
        }
        else
        {
            sender.sendMessage(PREFIX + ChatColor.RED + "KillauraEntity is disabled or not in on_command mode.");
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
    protected String[] getTabPossibilities()
    {
        return this.getPlayerNameTabs();
    }
}
