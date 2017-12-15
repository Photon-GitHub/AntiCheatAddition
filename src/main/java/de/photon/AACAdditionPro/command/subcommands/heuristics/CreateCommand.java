package de.photon.AACAdditionPro.command.subcommands.heuristics;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.checks.subchecks.InventoryHeuristics;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.heuristics.Graph;
import de.photon.AACAdditionPro.heuristics.OutputData;
import de.photon.AACAdditionPro.heuristics.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Queue;

public class CreateCommand extends InternalCommand
{
    private static final OutputData[] DEFAULT_OUTPUT_DATA = new OutputData[]{
            new OutputData("VANILLA"),
            new OutputData("CHEATING")
    };

    public CreateCommand()
    {
        super("create", InternalPermission.NEURAL_CREATE, (byte) 2);
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        if (AACAdditionPro.getInstance().getConfig().getBoolean("InventoryHeuristics.enabled"))
        {
            final String patternName = arguments.remove();

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
            InventoryHeuristics.getPATTERNS().add(new Pattern(patternName, new Graph(hiddenLayerConfig), DEFAULT_OUTPUT_DATA));
        }
    }

    @Override
    protected String[] getCommandHelp()
    {
        return new String[]{"Create a new pattern"};
    }

    @Override
    protected String[] getTabPossibilities()
    {
        return getChildTabs();
    }
}
