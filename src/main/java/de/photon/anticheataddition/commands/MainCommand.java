package de.photon.anticheataddition.commands;

import de.photon.anticheataddition.commands.subcommands.DebugCommand;
import de.photon.anticheataddition.commands.subcommands.InfoCommand;
import de.photon.anticheataddition.commands.subcommands.InternalTestCommand;
import de.photon.anticheataddition.commands.subcommands.SetVlCommand;
import de.photon.anticheataddition.util.messaging.ChatMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class MainCommand extends InternalCommand implements CommandExecutor, TabCompleter
{
    private final String acaVersion;

    public MainCommand(String acaVersion)
    {
        super("anticheataddition", CommandAttributes.builder()
                                                    .addCommandHelp("The main command of AntiCheatAddition", "To use a subcommands simply add it to the parent command:", "/anticheataddition <subcommand>")
                                                    .addChildCommands(new DebugCommand(),
                                                                      new InfoCommand(),
                                                                      new InternalTestCommand(),
                                                                      new SetVlCommand())
                                                    .build(), TabCompleteSupplier.builder());
        this.acaVersion = acaVersion;
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
        InternalCommand curCommand = this;
        InternalCommand child;
        for (String arg : args) {
            arg = arg.toLowerCase(Locale.ENGLISH);
            child = curCommand.getChildCommand(arg);
            // Automatic null and upper / lower case handling.
            if (child == null) return curCommand.getTabCompleteSupplier().getTabPossibilities(arg);
            curCommand = child;
        }
        return curCommand.getTabCompleteSupplier().getTabPossibilities();
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        ChatMessage.sendMessage(sender, "Version: " + this.acaVersion);
    }
}
