package de.photon.AACAdditionPro.command.subcommands.heuristics;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.checks.subchecks.InventoryHeuristics;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.command.subcommands.HeuristicsCommand;
import de.photon.AACAdditionPro.heuristics.NeuralPattern;
import de.photon.AACAdditionPro.heuristics.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Iterator;
import java.util.Queue;

public class ListCommand extends InternalCommand
{
    public ListCommand()
    {
        super("list", InternalPermission.NEURAL, (byte) 0);
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        if (AACAdditionPro.getInstance().getConfig().getBoolean("InventoryHeuristics.enabled"))
        {
            sender.sendMessage(HeuristicsCommand.HEURISTICS_HEADER);

            if (!InventoryHeuristics.getPATTERNS().isEmpty())
            {
                // High initial capacity to cover all the patterns easily
                final StringBuilder messageBuilder = new StringBuilder(256);

                messageBuilder.append(ChatColor.DARK_RED);
                messageBuilder.append("Active heuristics: ");
                messageBuilder.append(ChatColor.RED);

                final Iterator<Pattern> iterator = InventoryHeuristics.getPATTERNS().iterator();
                Pattern pattern = null;
                while (iterator.hasNext())
                {
                    // Prevent adding a comma at the very beginning of the enumeration.
                    if (pattern != null)
                    {
                        messageBuilder.append(", ");
                    }

                    pattern = iterator.next();
                    // New line
                    messageBuilder.append("\n");

                    // Name
                    messageBuilder.append(ChatColor.RED);
                    messageBuilder.append(pattern.getName());

                    messageBuilder.append(ChatColor.GOLD);
                    messageBuilder.append(" | ");

                    // Neurons
                    if (pattern instanceof NeuralPattern)
                    {
                        final NeuralPattern neuralPattern = (NeuralPattern) pattern;

                        // Subtract the input neurons and the output neuron
                        final int[] neuronsInLayers = neuralPattern.getGraph().getNeuronsInLayers();
                        messageBuilder.append(neuralPattern.getGraph().getMatrix().length - (neuronsInLayers[0] + neuronsInLayers[neuronsInLayers.length - 1]));
                        messageBuilder.append(" hidden Neurons");

                        messageBuilder.append(" | ");

                        // Layers
                        //  Subtract the input and output layer
                        messageBuilder.append(neuralPattern.getGraph().getNeuronsInLayers().length - 2);
                        messageBuilder.append(" hidden Layers");

                        messageBuilder.append(" | ");
                    }
                }

                sender.sendMessage(messageBuilder.toString());
            }
            else
            {
                sender.sendMessage(ChatColor.RED + "No active heuristics found.");
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
        return new String[]{"Lists all active heuristics."};
    }

    @Override
    protected String[] getTabPossibilities()
    {
        return getChildTabs();
    }
}
