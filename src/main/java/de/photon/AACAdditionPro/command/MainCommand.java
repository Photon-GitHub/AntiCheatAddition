package de.photon.AACAdditionPro.command;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.command.subcommands.EntityCheckCommand;
import de.photon.AACAdditionPro.command.subcommands.HeuristicsCommand;
import de.photon.AACAdditionPro.command.subcommands.InfoCommand;
import de.photon.AACAdditionPro.command.subcommands.TabListRemoveCommand;
import de.photon.AACAdditionPro.command.subcommands.VerboseCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

public class MainCommand extends InternalCommand implements CommandExecutor, TabCompleter
{
    public static final MainCommand instance = new MainCommand();

    private MainCommand()
    {
        super("aacadditionpro", null, (byte) 0,
              new EntityCheckCommand(),
              new HeuristicsCommand(),
              new InfoCommand(),
              new TabListRemoveCommand(),
              new VerboseCommand());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        this.invokeCommand(sender, new ArrayDeque<>(Arrays.asList(args)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        // Search for the deepest child command
        InternalCommand currentCommand = this;
        int currentArgumentIndex = 0;
        boolean childFound = true;
        while (currentArgumentIndex < args.length && childFound)
        {
            childFound = false;
            for (InternalCommand childCommand : currentCommand.getChildCommands())
            {
                if (childCommand.name.equalsIgnoreCase(args[currentArgumentIndex]))
                {
                    currentCommand = childCommand;
                    currentArgumentIndex++;
                    childFound = true;
                    break;
                }
            }
        }

        final int resultingArgumentIndex = currentArgumentIndex;
        return currentArgumentIndex < args.length ?
               // No tab filtering as the player has not started typing
               currentCommand.getTabPossibilities() :
               // If arguments are still left try to chose the correct tab possibilities from them.
               currentCommand.getTabPossibilities().stream().filter(tabPossibility -> tabPossibility.startsWith(args[resultingArgumentIndex])).collect(Collectors.toList());
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        sender.sendMessage(ChatColor.GOLD + AACAdditionPro.getInstance().getName() + " " + ChatColor.DARK_GRAY + AACAdditionPro.getInstance().getDescription().getVersion());
    }

    @Override
    protected String[] getCommandHelp()
    {
        return new String[]{"The main command of AACAdditionPro"};
    }

    @Override
    protected List<String> getTabPossibilities()
    {
        return getChildTabs();
    }
}
