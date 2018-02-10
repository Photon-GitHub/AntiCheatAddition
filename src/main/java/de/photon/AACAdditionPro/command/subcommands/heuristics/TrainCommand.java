package de.photon.AACAdditionPro.command.subcommands.heuristics;

import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.checks.subchecks.InventoryHeuristics;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.command.subcommands.HeuristicsCommand;
import de.photon.AACAdditionPro.heuristics.NeuralPattern;
import de.photon.AACAdditionPro.heuristics.Pattern;
import de.photon.AACAdditionPro.heuristics.TrainingData;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.VerboseSender;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Queue;

public class TrainCommand extends InternalCommand
{
    public TrainCommand()
    {
        super("train", InternalPermission.NEURAL_TRAIN, false, (byte) 2, (byte) 4);
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        if (HeuristicsCommand.heuristicsUnlocked())
        {
            final String patternName = arguments.remove();
            final Pattern pattern = InventoryHeuristics.getPatternByName(patternName);

            // The Heuristics Header will always be sent.
            sender.sendMessage(HeuristicsCommand.HEURISTICS_HEADER);

            if (pattern == null)
            {
                sender.sendMessage(HeuristicsCommand.createPatternNotFoundMessage(patternName));
            }
            else
            {
                // Cast the pattern
                if (!(pattern instanceof NeuralPattern))
                {
                    sender.sendMessage(ChatColor.GOLD + "Pattern " + ChatColor.RED + pattern.getName() + ChatColor.GOLD + " cannot be trained.");
                    return;
                }
                final NeuralPattern neuralPattern = (NeuralPattern) pattern;

                // Next argument
                final String playerOrFinishArgument = arguments.remove();

                if (playerOrFinishArgument.equalsIgnoreCase("finish"))
                {
                    neuralPattern.train();
                    sender.sendMessage(ChatColor.GOLD + "Pattern " + ChatColor.RED + neuralPattern.getName() + ChatColor.GOLD + " is now training.");
                    sender.sendMessage(ChatColor.GOLD + "Please watch the verbose - messages to see the end of training.");
                }
                else
                {
                    final Player trainingPlayer = Bukkit.getServer().getPlayer(playerOrFinishArgument);

                    if (trainingPlayer == null)
                    {
                        sender.sendMessage(PLAYER_NOT_FOUND_MESSAGE);
                        return;
                    }

                    final User user = UserManager.getUser(trainingPlayer.getUniqueId());

                    // Not bypassed
                    if (user == null)
                    {
                        sender.sendMessage(PLAYER_NOT_FOUND_MESSAGE);
                        return;
                    }

                    final String output = arguments.remove().toUpperCase();

                    for (String outputDataName : NeuralPattern.VALID_OUTPUTS)
                    {
                        if (outputDataName.equals(output))
                        {
                            user.getInventoryData().inventoryClicks.clear();

                            final TrainingData trainingData = new TrainingData(trainingPlayer.getUniqueId(), outputDataName);

                            // Override previous choices.
                            neuralPattern.getTrainingDataSet().remove(trainingData);
                            neuralPattern.getTrainingDataSet().add(trainingData);

                            final String messageString = ChatColor.GOLD + "[HEURISTICS] Training " + ChatColor.RED + patternName +
                                                         ChatColor.GOLD + " | Player: " + ChatColor.RED + trainingPlayer.getName() +
                                                         ChatColor.GOLD + " | Output: " + ChatColor.RED + output;

                            sender.sendMessage(messageString);
                            // .substring(13) to remove the [HEURISTICS] label.
                            VerboseSender.sendVerboseMessage(ChatColor.stripColor(messageString).substring(13));
                            return;
                        }
                    }

                    sender.sendMessage(ChatColor.GOLD + "Output \"" + ChatColor.RED + output + ChatColor.GOLD + "\"" + " is not allowed.");

                    final StringBuilder sb = new StringBuilder();
                    sb.append(ChatColor.GOLD);
                    sb.append("Allowed outputs: ");

                    for (String outputDataName : NeuralPattern.VALID_OUTPUTS)
                    {
                        sb.append(ChatColor.RED);
                        sb.append(outputDataName);
                        sb.append(ChatColor.GOLD);
                        sb.append(" | ");
                    }

                    // Delete the last " | ".
                    sb.delete(sb.length() - 2, sb.length());
                    sender.sendMessage(sb.toString());
                }
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
                "Train a pattern with an example.",
                "Train syntax  : /aacadditionpro train <name of pattern> <player to learn from> <output>",
                "Finish syntax : /aacadditionpro train <name of pattern> finish"
        };
    }

    @Override
    protected String[] getTabPossibilities()
    {
        return new String[]{
                "VANILLA",
                "CHEATING"
        };
    }
}
