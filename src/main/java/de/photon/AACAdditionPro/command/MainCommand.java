package de.photon.AACAdditionPro.command;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.command.subcommands.HeuristicsCommand;
import de.photon.AACAdditionPro.command.subcommands.InfoCommand;
import de.photon.AACAdditionPro.command.subcommands.VerboseCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public final class MainCommand extends InternalCommand implements CommandExecutor, TabCompleter
{

    public static final MainCommand instance = new MainCommand();

    private MainCommand()
    {
        super("aacadditionpro", (byte) 0, false, null, "The main command of AACAdditionPro");
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] arguments)
    {
        if (command.getName().equalsIgnoreCase(this.commandName)) {
            if (arguments.length == 0) {
                sender.sendMessage(ChatColor.GOLD + AACAdditionPro.getInstance().getName() + " " + ChatColor.DARK_GRAY + AACAdditionPro.getInstance().getDescription().getVersion());
                return true;
            }

            if ("?".equals(arguments[0])) {
                sender.sendMessage(prefix + ChatColor.GOLD + "The main command of AACAdditionPro");
                return true;
            }

            //Delegate the sub-commands
            for (final InternalCommand cmd : this.getChildCommands()) {
                if (cmd.commandName.equalsIgnoreCase(arguments[0])) {
                    final LinkedList<String> argumentsLeft = new LinkedList<>(Arrays.asList(arguments));
                    argumentsLeft.removeFirst();

                    cmd.onSubCommand(sender, argumentsLeft);
                    return true;
                }
            }

            sender.sendMessage(ChatColor.DARK_RED + "Command not found.");
            return true;
        }
        return false;
    }

    protected HashSet<InternalCommand> getChildCommands()
    {
        return new HashSet<>(Arrays.asList(
                new InfoCommand(),
                new VerboseCommand(),
                new HeuristicsCommand())
        );
    }

    @Override
    public List<String> onTabComplete(final CommandSender commandSender, final Command command, final String label, final String[] arguments)
    {
        final List<String> tab = new ArrayList<>();

        InternalCommand current = this;

        //Get the current InternalCommand from the arguments
        for (final String s : arguments) {
            if (s == null || s.isEmpty() || current.getChildCommands() == null || current.getChildCommands().isEmpty()) {
                break;
            }

            for (final InternalCommand cmd : current.getChildCommands()) {
                if (cmd.commandName.equalsIgnoreCase(s)) {
                    current = cmd;
                    break;
                }
            }
        }

        final ArrayDeque<String> commandAndArguments = new ArrayDeque<>();
        commandAndArguments.add(command.getName());
        Collections.addAll(commandAndArguments, arguments);

        //Get the correct argument
        final String lastArgument = commandAndArguments.removeLast();

        if (current.commandName.equalsIgnoreCase(lastArgument)) {
            //Iterate through the TabPossibilities of the InternalCommand
            for (final String s : current.getTabPossibilities()) {
                if (s != null && !s.isEmpty()) {
                    tab.add(s);
                }
            }
        } else if (!commandAndArguments.isEmpty() && current.commandName.equalsIgnoreCase(commandAndArguments.getLast())) {
            //Iterate through the TabPossibilities of the InternalCommand
            for (final String s : current.getTabPossibilities()) {
                //String is valid
                if (s != null && !s.isEmpty() && !" ".equals(s)
                    //String-Start
                    && s.startsWith(lastArgument))
                {
                    tab.add(s);
                }
            }
        }
        return tab;
    }
}