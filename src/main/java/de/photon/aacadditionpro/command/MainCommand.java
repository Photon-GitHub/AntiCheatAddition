package de.photon.aacadditionpro.command;

import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.command.subcommands.InfoCommand;
import de.photon.aacadditionpro.command.subcommands.TabListRemoveCommand;
import de.photon.aacadditionpro.command.subcommands.VerboseCommand;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

public class MainCommand extends InternalCommand implements CommandExecutor, TabCompleter
{
    @Getter
    private static final MainCommand instance = new MainCommand();

    private MainCommand()
    {
        super("aacadditionpro",
              null,
              CommandAttributes.builder()
                               .addCommandHelpLine("The main command of AACAdditionPro")
                               .build(),
              ImmutableSet.of(new InfoCommand(),
                              new TabListRemoveCommand(),
                              new VerboseCommand()),
              TabCompleteSupplier.builder());
    }

    public String getMainCommandName()
    {
        return this.name;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args)
    {
        this.invokeCommand(sender, new ArrayDeque<>(Arrays.asList(args)));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args)
    {
        // Search for the deepest child command
        InternalCommand currentCommand = this;
        InternalCommand potentialChildCommand;
        int currentArgumentIndex = 0;
        while (currentArgumentIndex < args.length) {
            potentialChildCommand = currentCommand.childCommands.get(args[currentArgumentIndex].toLowerCase(Locale.ENGLISH));

            if (potentialChildCommand == null) {
                // Stop looping once no child command was found.
                break;
            }

            currentCommand = potentialChildCommand;
            ++currentArgumentIndex;
        }


        return currentArgumentIndex == args.length ?
               // No tab filtering as the player has not started typing
               currentCommand.getTabCompleteSupplier().getTabPossibilities() :
               // Player has started typing.
               currentCommand.getTabCompleteSupplier().getTabPossibilities(args[currentArgumentIndex]);
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        sender.sendMessage(ChatColor.GOLD + AACAdditionPro.getInstance().getName() + " " + ChatColor.DARK_GRAY + AACAdditionPro.getInstance().getDescription().getVersion());
    }
}
