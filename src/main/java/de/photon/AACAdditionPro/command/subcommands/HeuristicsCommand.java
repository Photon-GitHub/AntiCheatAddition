package de.photon.AACAdditionPro.command.subcommands;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.command.subcommands.heuristics.CreateCommand;
import de.photon.AACAdditionPro.command.subcommands.heuristics.ListCommand;
import de.photon.AACAdditionPro.command.subcommands.heuristics.RemoveCommand;
import de.photon.AACAdditionPro.command.subcommands.heuristics.RenameCommand;
import de.photon.AACAdditionPro.command.subcommands.heuristics.TrainCommand;
import de.photon.AACAdditionPro.modules.checks.InventoryHeuristics;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Queue;

public class HeuristicsCommand extends InternalCommand
{
    /**
     * The constant for the {@link InventoryHeuristics}' interactions with the chat.
     * This is the header that appears on top of each message block sent by the {@link InventoryHeuristics}.
     */
    public static final String HEURISTICS_HEADER = ChatColor.GOLD + "------" + ChatColor.DARK_RED + " Heuristics - Pattern " + ChatColor.GOLD + "------";
    public static final String FRAMEWORK_DISABLED = PREFIX + ChatColor.RED + "InventoryHeuristics framework is not loaded, enabled or unlocked.";

    public static boolean heuristicsUnlocked()
    {
        return AACAdditionPro.getInstance().getConfig().getBoolean("InventoryHeuristics.enabled") &&
               AACAdditionPro.getInstance().getConfig().getBoolean("InventoryHeuristics.unlock_full_framework");
    }

    public HeuristicsCommand()
    {
        super("heuristics",
              InternalPermission.NEURAL,
              new CreateCommand(),
              new ListCommand(),
              new RemoveCommand(),
              new RenameCommand(),
              new TrainCommand());
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        if (!heuristicsUnlocked())
        {
            sendErrorMessage(sender, "InventoryHeuristics framework is not loaded, enabled or unlocked.");
        }
        else
        {
            sender.sendMessage(HEURISTICS_HEADER);
            sender.sendMessage(ChatColor.GOLD + "Welcome to the heuristics framework.");
            sender.sendMessage(ChatColor.RED + "Possible commands: " + ChatColor.GOLD + String.join(", ", this.childTabs));
        }
    }

    /**
     * Creates a new pattern not found message with {@link ChatColor}s.
     *
     * @param nameOfPattern the name of the pattern that should be used for the message.
     */
    public static String createPatternNotFoundMessage(final String nameOfPattern)
    {
        return ChatColor.GOLD +
               "Pattern \"" +
               ChatColor.RED +
               nameOfPattern +
               ChatColor.GOLD +
               "\" could not be found.";
    }

    @Override
    protected String[] getCommandHelp()
    {
        return new String[]{"Utilities for the InventoryHeuristics"};
    }
}
