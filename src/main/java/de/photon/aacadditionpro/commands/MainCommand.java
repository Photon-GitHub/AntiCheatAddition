package de.photon.aacadditionpro.commands;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.commands.subcommands.DebugCommand;
import de.photon.aacadditionpro.util.messaging.ChatMessage;
import lombok.Getter;
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

    public MainCommand()
    {
        super("aacadditionpro", CommandAttributes.builder()
                                                 .addCommandHelp("The main command of AACAdditionPro", "To use a subcommands simply add it to the parent command:", "/aacadditionpro <subcommand>")
                                                 .addChildCommands(new DebugCommand())
                                                 .build(), TabCompleteSupplier.builder());
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
        ChatMessage.sendMessage(sender, "Version: " + AACAdditionPro.getInstance().getDescription().getVersion());
    }
}
