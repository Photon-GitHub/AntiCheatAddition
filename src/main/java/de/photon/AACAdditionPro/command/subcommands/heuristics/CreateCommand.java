package de.photon.AACAdditionPro.command.subcommands.heuristics;

import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.checks.subchecks.InventoryHeuristics;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.command.subcommands.HeuristicsCommand;
import de.photon.AACAdditionPro.heuristics.Graph;
import de.photon.AACAdditionPro.heuristics.InputData;
import de.photon.AACAdditionPro.heuristics.OutputData;
import de.photon.AACAdditionPro.heuristics.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;

public class CreateCommand extends InternalCommand
{
    private final HashMap<String, InputData> inputDataMapping = new HashMap<>(6, 1);

    public CreateCommand()
    {
        super("create", InternalPermission.NEURAL_CREATE, (byte) 3);

        inputDataMapping.put("T", new InputData("TIMEDELTAS"));
        inputDataMapping.put("M", new InputData("MATERIALS"));
        inputDataMapping.put("R", new InputData("RAWSLOTS"));
        inputDataMapping.put("I", new InputData("INVENTORYTYPES"));
        inputDataMapping.put("S", new InputData("SLOTTYPES"));
        inputDataMapping.put("C", new InputData("CLICKTYPES"));
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        if (HeuristicsCommand.heurisitcsUnlocked())
        {
            final String patternName = arguments.remove();

            /* Valid ones:
             * T = TimeDeltas
             * M = Materials
             * R = RawSlots
             * I = InventoryType
             * S = SlotTypes
             * C = ClickTypes
             * */
            final String encodedInputs = arguments.remove();
            final List<InputData> inputDataList = new ArrayList<>(6);

            inputDataMapping.forEach(
                    (keyChar, data) ->
                    {
                        if (encodedInputs.contains(keyChar))
                        {
                            inputDataList.add(data);
                        }
                    });

            if (InventoryHeuristics.getPATTERNS().stream().anyMatch(pattern -> pattern.getName().equals(patternName)))
            {
                sender.sendMessage(ChatColor.GOLD + "------" + ChatColor.DARK_RED + " Heuristics - Pattern " + ChatColor.GOLD + "------");
                sender.sendMessage(ChatColor.GOLD + "Pattern name \"" + patternName + "\"" + " is already in use.");
            }

            final List<String> hiddenLayerConfigStrings = new ArrayList<>(arguments);
            int[] hiddenLayerConfig = new int[hiddenLayerConfigStrings.size()];

            for (int i = 0; i < hiddenLayerConfigStrings.size(); i++)
            {
                hiddenLayerConfig[i] = Integer.valueOf(hiddenLayerConfigStrings.get(i));
            }

            sender.sendMessage(ChatColor.GOLD + "------" + ChatColor.DARK_RED + " Heuristics - Pattern " + ChatColor.GOLD + "------");
            sender.sendMessage(ChatColor.GOLD + "Created new pattern \"" + ChatColor.RED + patternName + ChatColor.GOLD + "\"" + " with " + hiddenLayerConfig.length + " layers and " + inputDataList.size() + " inputs.");

            InventoryHeuristics.getPATTERNS().add(new Pattern(
                    patternName,
                    new Graph(hiddenLayerConfig),
                    inputDataList.toArray(new InputData[inputDataList.size()]),
                    OutputData.DEFAULT_OUTPUT_DATA));
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
                "Format: /aacadditionpro create <Name of pattern> <Inputs> <Neurons of layer 1> <Neurons of layer 2> ..."
        };
    }

    @Override
    protected String[] getTabPossibilities()
    {
        return getChildTabs();
    }
}
