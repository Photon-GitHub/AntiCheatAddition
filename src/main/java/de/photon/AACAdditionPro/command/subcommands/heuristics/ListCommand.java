package de.photon.AACAdditionPro.command.subcommands.heuristics;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.checks.subchecks.InventoryHeuristics;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.command.subcommands.HeuristicsCommand;
import de.photon.AACAdditionPro.heuristics.InputData;
import de.photon.AACAdditionPro.heuristics.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Map;
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
                messageBuilder.append("Active heuristics: \n");
                messageBuilder.append(ChatColor.RED);

                for (Pattern pattern : InventoryHeuristics.getPATTERNS())
                {
                    // New line
                    messageBuilder.append("\n");

                    // Name
                    messageBuilder.append(ChatColor.RED);
                    messageBuilder.append(pattern.getName());

                    messageBuilder.append(ChatColor.GOLD);
                    messageBuilder.append(" | ");

                    // Neurons
                    messageBuilder.append(ChatColor.RED);
                    messageBuilder.append(pattern.getGraph().getNeurons().length);
                    messageBuilder.append(" Neurons");

                    messageBuilder.append(ChatColor.GOLD);
                    messageBuilder.append(" | ");

                    // Layers
                    messageBuilder.append(ChatColor.RED);
                    messageBuilder.append(pattern.getGraph().getNeuronsInLayers().length);
                    messageBuilder.append(" Layers");

                    messageBuilder.append(ChatColor.GOLD);
                    messageBuilder.append(" | ");

                    // Inputs
                    messageBuilder.append(ChatColor.RED);
                    for (InputData inputData : pattern.getInputs())
                    {
                        // Find the character in the map.
                        for (Map.Entry<Character, InputData> characterInputDataEntry : InputData.VALID_INPUTS.entrySet())
                        {
                            if (characterInputDataEntry.getValue().getName().equals(inputData.getName()))
                            {
                                // Write correct char
                                messageBuilder.append(characterInputDataEntry.getKey());
                                break;
                            }
                        }
                    }

                    messageBuilder.append(ChatColor.GOLD);
                    messageBuilder.append(", ");
                }

                // Delete the last comma and space
                for (int i = 0; i < 2; i++)
                {
                    messageBuilder.deleteCharAt(messageBuilder.length() - 1);
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
