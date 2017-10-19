package de.photon.AACAdditionPro.command;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.command.subcommands.InfoCommand;
import de.photon.AACAdditionPro.command.subcommands.VerboseCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;

public class MainCommand extends InternalCommand implements CommandExecutor, TabCompleter
{
    public static final MainCommand instance = new MainCommand();

    private MainCommand()
    {
        super("aacadditionpro", (byte) 0);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (command.getName().equalsIgnoreCase(this.name))
        {
            this.invokeCommand(sender, new LinkedList<>(Arrays.asList(args)));
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        InternalCommand currentCommand = this;
        final Queue<String> allArguments = new LinkedList<>(Arrays.asList(args));

        String currentArgument;
        while (!allArguments.isEmpty() && currentCommand.getChildCommands() != null)
        {
            currentArgument = allArguments.remove();

            InternalCommand lastCommand = currentCommand;
            for (final InternalCommand childCommand : currentCommand.getChildCommands())
            {
                if (childCommand.name.equalsIgnoreCase(currentArgument))
                {
                    currentCommand = childCommand;
                    break;
                }
            }

            // Stop the loop if you cannot go on.
            if (lastCommand.equals(currentCommand))
            {
                break;
            }
        }

        // The final result
        List<String> tab;

        // The args are only child commands so far
        if (allArguments.isEmpty())
        {
            tab = Arrays.asList(currentCommand.getTabPossibilities());
        }
        // Probably began some typing of an argument
        else
        {
            tab = new ArrayList<>(currentCommand.getTabPossibilities().length);
            for (String tabPossiblitity : currentCommand.getTabPossibilities())
            {
                if (tabPossiblitity.startsWith(allArguments.peek()))
                {
                    tab.add(tabPossiblitity);
                }
            }
        }

        return tab;
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
    protected Set<InternalCommand> getChildCommands()
    {
        return new HashSet<>(Arrays.asList(
                // Disabled for now
                // new HeuristicsCommand(),
                new InfoCommand(),
                new VerboseCommand()));
    }

    @Override
    protected String[] getTabPossibilities()
    {
        return getChildTabs();
    }
}
