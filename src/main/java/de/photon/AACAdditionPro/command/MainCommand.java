package de.photon.AACAdditionPro.command;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.command.subcommands.EntityCheckCommand;
import de.photon.AACAdditionPro.command.subcommands.HeuristicsCommand;
import de.photon.AACAdditionPro.command.subcommands.InfoCommand;
import de.photon.AACAdditionPro.command.subcommands.TabListRemoveCommand;
import de.photon.AACAdditionPro.command.subcommands.VerboseCommand;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

public class MainCommand extends InternalCommand implements CommandExecutor, TabCompleter
{
    @Getter
    private static final MainCommand instance = new MainCommand();

    private MainCommand()
    {
        super("aacadditionpro",
              null,
              new EntityCheckCommand(),
              new HeuristicsCommand(),
              new InfoCommand(),
              new TabListRemoveCommand(),
              new VerboseCommand());
    }

    public String getMainCommandName()
    {
        return this.name;
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
        InternalCommand potentialChildCommand;
        int currentArgumentIndex = 0;
        while (currentArgumentIndex < args.length) {
            potentialChildCommand = currentCommand.getChildCommandByNameIgnoreCase(args[currentArgumentIndex]);

            if (potentialChildCommand == null) {
                // Stop looping once no child command was found.
                break;
            }

            currentCommand = potentialChildCommand;
            currentArgumentIndex++;

        }

        final List<String> tabs = currentCommand.getTabPossibilities();

        // No tab filtering as the player has not started typing
        if (currentArgumentIndex == args.length) {
            return tabs;
        }

        // If arguments are still left try to choose the correct tab possibilities from them.
        final List<String> tabPossibilities = new ArrayList<>(tabs.size());
        for (String tabPossibility : tabs) {
            if (tabPossibility.startsWith(args[currentArgumentIndex])) {
                tabPossibilities.add(tabPossibility);
            }
        }
        return tabPossibilities;

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
}
