package de.photon.AACAdditionPro.command.subcommands.heuristics;

import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.checks.subchecks.InventoryHeuristics;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.command.subcommands.HeuristicsCommand;
import de.photon.AACAdditionPro.heuristics.Input;
import de.photon.AACAdditionPro.heuristics.NeuralPattern;
import de.photon.AACAdditionPro.neural.ActivationFunctions;
import de.photon.AACAdditionPro.neural.Graph;
import de.photon.AACAdditionPro.util.datawrappers.InventoryClick;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

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

                final int epoch = Integer.valueOf(arguments.remove());
                final double trainParameter = Double.valueOf(arguments.remove());
                final double momentum = Double.valueOf(arguments.remove());

                final Input.InputType[] inputTypes = Input.InputType.parseInputTypesFromArgument(arguments.remove());

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

                    sender.sendMessage(ChatColor.GOLD + "Created new pattern \"" + ChatColor.RED + patternName + ChatColor.GOLD + "\"" + " with " + hiddenLayerConfig.length + " hidden layers and " + inputTypes.length + " inputs.");

                    InventoryHeuristics.getPATTERNS().add(new NeuralPattern(patternName, inputTypes, Graph.builder()
                                                                                                          .setEpoch(epoch)
                                                                                                          .setTrainParameter(trainParameter)
                                                                                                          .setMomentum(momentum)
                                                                                                          .setInputNeurons(InventoryClick.SAMPLES * inputTypes.length)
                                                                                                          .addHiddenLayers(hiddenLayerConfig)
                                                                                                          .addOutput("vanilla")
                                                                                                          .addOutput("cheating")
                                                                                                          .setActivationFunction(ActivationFunctions.HYPERBOLIC_TANGENT)
                                                                                                          .build()));
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
                "Creates a new pattern with a clear graph. Training is required if you want the pattern to be permanent.",
                "Format: /aacadditionpro heuristics create <name of pattern> <epoch> <train parameter> <momentum> <inputs> <neuron count of layer 1> <neuron count of layer 2> ...",
                "You may use any combination of the following letters as a valid input specification:",
                "T = TimeDeltas | M = Materials | X = X-Distances | Y = Y-Distances | C = ClickTypes",
                "Examples: TXYM, TM, CT, TMYXC"
        };
    }

    @Override
    protected String[] getTabPossibilities()
    {
        return getChildTabs();
    }
}
