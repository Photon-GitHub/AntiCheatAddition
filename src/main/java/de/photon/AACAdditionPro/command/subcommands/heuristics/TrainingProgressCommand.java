package de.photon.AACAdditionPro.command.subcommands.heuristics;

import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.checks.subchecks.InventoryHeuristics;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.command.subcommands.HeuristicsCommand;
import de.photon.AACAdditionPro.heuristics.Pattern;
import de.photon.AACAdditionPro.heuristics.TrainingData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class TrainingProgressCommand extends InternalCommand
{
    public TrainingProgressCommand()
    {
        super("progress", InternalPermission.NEURAL_SAVE, (byte) 1);
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        if (HeuristicsCommand.heuristicsUnlocked())
        {
            final String patternName = arguments.remove();
            final Set<Pattern> possiblePatterns = InventoryHeuristics.getPATTERNS().stream().filter(pattern -> pattern.getName().equals(patternName)).collect(Collectors.toSet());

            if (possiblePatterns.isEmpty())
            {
                sender.sendMessage(HeuristicsCommand.HEURISTICS_HEADER);
                sender.sendMessage(ChatColor.GOLD + "Pattern \"" + ChatColor.RED + patternName + ChatColor.GOLD + "\"" + " could not be found.");
            }
            else if (possiblePatterns.size() > 1)
            {
                throw new IllegalStateException("Two heuristics with the same name exist.");
            }
            else
            {
                final Player trainingPlayer = Bukkit.getServer().getPlayer(arguments.remove());

                if (trainingPlayer == null)
                {
                    sender.sendMessage(playerNotFoundMessage);
                    return;
                }

                sender.sendMessage(HeuristicsCommand.HEURISTICS_HEADER);

                for (Pattern possiblePattern : possiblePatterns)
                {
                    Optional<TrainingData> optionalTrainingData = possiblePattern.getTrainingDataSet().stream().filter(trainingData -> trainingData.getUuid().equals(trainingPlayer.getUniqueId())).findAny();

                    final StringBuilder sb = new StringBuilder();
                    sb.append(ChatColor.GOLD);
                    sb.append("Player ");
                    sb.append(ChatColor.RED);
                    sb.append(trainingPlayer);
                    sb.append(ChatColor.GOLD);


                    if (optionalTrainingData.isPresent())
                    {
                        sb.append(" has ");
                        sb.append(ChatColor.RED);
                        sb.append(optionalTrainingData.get().trainingCycles);
                        sb.append(ChatColor.GOLD);
                        sb.append(" cycles left.");
                    }
                    else
                    {
                        sb.append(" does not train pattern ");
                        sb.append(ChatColor.RED);
                        sb.append(possiblePattern.getName());
                    }

                    sender.sendMessage(sb.toString());
                }
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
        return new String[]{"Displays the progress of a player who is training a pattern."};
    }

    @Override
    protected String[] getTabPossibilities()
    {
        return getChildTabs();
    }
}
