package de.photon.AACAdditionPro.command.subcommands.heuristics;

import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.checks.subchecks.InventoryHeuristics;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.command.subcommands.HeuristicsCommand;
import de.photon.AACAdditionPro.heuristics.InputData;
import de.photon.AACAdditionPro.heuristics.OutputData;
import de.photon.AACAdditionPro.heuristics.Pattern;
import de.photon.AACAdditionPro.util.storage.datawrappers.InventoryClick;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class CreateCommand extends InternalCommand
{

    public CreateCommand()
    {
        super("create", InternalPermission.NEURAL_CREATE, (byte) 3);
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        if (HeuristicsCommand.heuristicsUnlocked())
        {
            try
            {
                final String patternName = arguments.remove();
                // A full list of all allowed inputs can be found as InputData.VALID_INPUTS
                final String encodedInputs = arguments.remove();
                final List<InputData> inputDataList = new ArrayList<>(6);

                InputData.VALID_INPUTS.forEach(
                        (keyChar, data) ->
                        {
                            if (encodedInputs.contains(keyChar))
                            {
                                inputDataList.add(data);
                            }
                        });

                if (InventoryHeuristics.getPATTERNS().stream().anyMatch(pattern -> pattern.getName().equals(patternName)))
                {
                    sender.sendMessage(HeuristicsCommand.HEURISTICS_HEADER);
                    sender.sendMessage(ChatColor.GOLD + "Pattern name \"" + patternName + "\"" + " is already in use.");
                    return;
                }

                final List<String> hiddenLayerConfigStrings = new ArrayList<>(arguments);
                int[] hiddenLayerConfig = new int[hiddenLayerConfigStrings.size()];

                for (int i = 0; i < hiddenLayerConfigStrings.size(); i++)
                {
                    hiddenLayerConfig[i] = Integer.valueOf(hiddenLayerConfigStrings.get(i));
                }

                sender.sendMessage(HeuristicsCommand.HEURISTICS_HEADER);
                sender.sendMessage(ChatColor.GOLD + "Created new pattern \"" + ChatColor.RED + patternName + ChatColor.GOLD + "\"" + " with " + hiddenLayerConfig.length + " hidden layers and " + inputDataList.size() + " inputs.");

                InventoryHeuristics.getPATTERNS().add(new Pattern(
                        patternName,
                        inputDataList.toArray(new InputData[inputDataList.size()]),
                        InventoryClick.SAMPLES,
                        OutputData.DEFAULT_OUTPUT_DATA,
                        hiddenLayerConfig));

            } catch (NumberFormatException exception)
            {
                sender.sendMessage(prefix + ChatColor.RED + "Formatting error. Please utilize the command help for formatting.");
            }
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
                "Creates a new pattern with a clear graph. Saving is required if you want the pattern to be permanent.",
                "Format: /aacadditionpro create <name of pattern> <inputs> <neuron count of layer 1> <neuron count of layer 2> ...",
                "You may use any combination of the following letters as a valid input specification.",
                "Guide to the letters: T = TimeDeltas | M = Materials | X = X-Distances | Y = Y-Distances | I = InventoryType | S = SlotTypes | C = ClickTypes",
                "Examples: TRM, TM, RM, CS, TMRISC"
        };
    }

    @Override
    protected String[] getTabPossibilities()
    {
        return getChildTabs();
    }
}
