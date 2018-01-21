package de.photon.AACAdditionPro.command.subcommands.heuristics;

import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.checks.subchecks.InventoryHeuristics;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.command.subcommands.HeuristicsCommand;
import de.photon.AACAdditionPro.heuristics.InputData;
import de.photon.AACAdditionPro.heuristics.NeuralPattern;
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

                // The Heuristics Header will always be sent.
                sender.sendMessage(HeuristicsCommand.HEURISTICS_HEADER);

                final String patternName = arguments.remove();
                // A full list of all allowed inputs can be found as InputData.VALID_INPUTS
                final String encodedInputs = arguments.remove();
                final List<InputData> inputDataList = new ArrayList<>(6);

                // Search for the characters and add the InputData if necessary.
                for (char c : encodedInputs.toCharArray())
                {
                    final char upChar = Character.toUpperCase(c);
                    final InputData inputData = InputData.VALID_INPUTS.get(upChar);
                    if (inputData == null)
                    {
                        sender.sendMessage(ChatColor.GOLD + "Could not create pattern as an invalid input was provided: \"" + upChar + "\"");
                        return;
                    }
                    else
                    {
                        inputDataList.add(inputData);
                    }
                }

                if (InventoryHeuristics.getPATTERNS().stream().anyMatch(pattern -> pattern.getName().equals(patternName)))
                {
                    sender.sendMessage(ChatColor.GOLD + "Pattern name \"" + patternName + "\"" + " is already in use.");
                }
                else
                {
                    int[] hiddenLayerConfig = new int[arguments.size()];

                    for (int i = 0; i < hiddenLayerConfig.length; i++)
                    {
                        try
                        {
                            hiddenLayerConfig[i] = Integer.valueOf(arguments.remove());
                        } catch (NumberFormatException exception)
                        {
                            sender.sendMessage(PREFIX + ChatColor.RED + "Unable to parse the neuron count to an integer.");
                            return;
                        }
                    }

                    sender.sendMessage(ChatColor.GOLD + "Created new pattern \"" + ChatColor.RED + patternName + ChatColor.GOLD + "\"" + " with " + hiddenLayerConfig.length + " hidden layers and " + inputDataList.size() + " inputs.");

                    InventoryHeuristics.getPATTERNS().add(new NeuralPattern(
                            patternName,
                            inputDataList.toArray(new InputData[inputDataList.size()]),
                            InventoryClick.SAMPLES,
                            hiddenLayerConfig));
                }
            } catch (NumberFormatException exception)
            {
                sender.sendMessage(PREFIX + ChatColor.RED + "Formatting error. Please utilize the command help for formatting.");
            }
        }
        else
        {
            sender.sendMessage(HeuristicsCommand.FRAMEWORK_DISABLED);
        }
    }

    @Override
    protected String[] getCommandHelp()
    {
        return new String[]{
                "Creates a new pattern with a clear graph. Saving is required if you want the pattern to be permanent.",
                "Format: /aacadditionpro heuristics create <name of pattern> <inputs> <neuron count of layer 1> <neuron count of layer 2> ...",
                "You may use any combination of the following letters as a valid input specification.",
                "Guide to the letters: T = TimeDeltas | M = Materials | X = X-Distances | Y = Y-Distances | I = InventoryType | S = SlotTypes | C = ClickTypes",
                "Examples: TXYM, TM, RM, CS, TMYXISC"
        };
    }

    @Override
    protected String[] getTabPossibilities()
    {
        return getChildTabs();
    }
}
