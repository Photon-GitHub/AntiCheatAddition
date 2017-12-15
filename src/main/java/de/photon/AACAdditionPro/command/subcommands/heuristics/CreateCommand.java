package de.photon.AACAdditionPro.command.subcommands.heuristics;

import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.checks.subchecks.InventoryHeuristics;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.command.subcommands.HeuristicsCommand;
import de.photon.AACAdditionPro.heuristics.Graph;
import de.photon.AACAdditionPro.heuristics.OutputData;
import de.photon.AACAdditionPro.heuristics.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Queue;

public class CreateCommand extends InternalCommand
{
    public CreateCommand()
    {
        super("create", InternalPermission.NEURAL_CREATE, (byte) 2);
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        if (HeuristicsCommand.heurisitcsUnlocked())
        {
            final String patternName = arguments.remove();

            if (InventoryHeuristics.getPATTERNS().stream().anyMatch(pattern -> pattern.getName().equals(patternName)))
            {
                sender.sendMessage(ChatColor.GOLD + "------" + ChatColor.DARK_RED + " Heuristics - Pattern " + ChatColor.GOLD + "------");
                sender.sendMessage(ChatColor.GOLD + "Pattern name \"" + patternName + "\"" + " is already in use.");
            }

            final StringBuilder hiddenLayerConfigBuilder = new StringBuilder();

            while (!arguments.isEmpty())
            {
                hiddenLayerConfigBuilder.append(arguments.remove());
            }

            String[] hiddenLayerConfigStrings = hiddenLayerConfigBuilder.toString().split(" ");

            int[] hiddenLayerConfig = new int[hiddenLayerConfigStrings.length];

            for (int i = 0; i < hiddenLayerConfigStrings.length; i++)
            {
                hiddenLayerConfig[i] = Integer.valueOf(hiddenLayerConfigStrings[i]);
            }

            sender.sendMessage(ChatColor.GOLD + "------" + ChatColor.DARK_RED + " Heuristics - Pattern " + ChatColor.GOLD + "------");
            sender.sendMessage(ChatColor.GOLD + "Created new Pattern \"" + patternName + "\"" + " with " + hiddenLayerConfig.length + " layers.");
            InventoryHeuristics.getPATTERNS().add(new Pattern(patternName, new Graph(hiddenLayerConfig), OutputData.DEFAULT_OUTPUT_DATA));
        }
        else
        {
            sender.sendMessage(prefix + ChatColor.RED + "InventoryHeuristics framework is not loaded, enabled or unlocked.");
        }
    }

    @Override
    protected String[] getCommandHelp()
    {
        return new String[]{
                "Creates a new pattern with a clear graph. Saving is required if you want the pattern to be permanent."
        };
    }

    @Override
    protected String[] getTabPossibilities()
    {
        return getChildTabs();
    }
}
