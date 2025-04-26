package de.photon.anticheataddition.commands;

import de.photon.anticheataddition.commands.subcommands.DebugCommand;
import de.photon.anticheataddition.commands.subcommands.InfoCommand;
import de.photon.anticheataddition.commands.subcommands.InternalTestCommand;
import de.photon.anticheataddition.commands.subcommands.ReloadCommand;      // <─ NEW
import de.photon.anticheataddition.commands.subcommands.SetVlCommand;
import de.photon.anticheataddition.util.messaging.ChatMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Root command: /anticheataddition
 */
public final class MainCommand extends InternalCommand implements CommandExecutor, TabCompleter {

    private final String acaVersion;

    public MainCommand(String acaVersion) {
        super("anticheataddition",
              CommandAttributes.builder()
                               // put any fancy help-text / permission calls here if you need them
                               .addChildCommands(
                                   new DebugCommand(),
                                   new InfoCommand(),
                                   new InternalTestCommand(),
                                   new SetVlCommand(),
                                   new ReloadCommand())        // <─ NEW
                               .build(),
              TabCompleteSupplier.builder());

        this.acaVersion = acaVersion;
    }

    /* ---------------------------------------------------------------------- */
    /* Bukkit command boiler-plate                                            */
    /* ---------------------------------------------------------------------- */

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        // Delegate to the internal command framework
        this.invokeCommand(sender, new ArrayDeque<>(Arrays.asList(args)));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      String[] args) {
        InternalCommand cur = this;
        InternalCommand child;

        for (String arg : args) {
            arg = arg.toLowerCase(Locale.ENGLISH);
            child = cur.getChildCommand(arg);

            // unknown sub-command → return matches for current depth
            if (child == null) {
                return cur.getTabCompleteSupplier().getTabPossibilities(arg);
            }
            cur = child;
        }
        return cur.getTabCompleteSupplier().getTabPossibilities();
    }

    /* ---------------------------------------------------------------------- */
    /* Top-level fallback: /anticheataddition (no sub-command)                 */
    /* ---------------------------------------------------------------------- */

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments) {
        ChatMessage.sendMessage(sender, "Version: " + this.acaVersion);
    }
}
