package de.photon.AACAdditionPro.command.subcommands.heuristics;

import com.google.common.collect.ImmutableList;
import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.checks.subchecks.InventoryHeuristics;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.command.subcommands.HeuristicsCommand;
import de.photon.AACAdditionPro.heuristics.NeuralPattern;
import de.photon.AACAdditionPro.heuristics.Pattern;
import de.photon.AACAdditionPro.neural.Output;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.VerboseSender;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
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
            final Pattern pattern = InventoryHeuristics.PATTERNS.get(patternName);

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
                final Player trainingPlayer = Bukkit.getServer().getPlayer(arguments.remove());

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

                final String label = arguments.remove();

                if (Arrays.stream(Pattern.LEGIT_OUTPUT).noneMatch(possibleOutput -> possibleOutput.getLabel().equals(label)))
                {
                    sender.sendMessage(ChatColor.GOLD + "Output \"" + ChatColor.RED + label + ChatColor.GOLD + "\"" + " is not allowed.");

                    final StringBuilder sb = new StringBuilder();
                    sb.append(ChatColor.GOLD);
                    sb.append("Allowed outputs: ");

                    for (Output output : NeuralPattern.LEGIT_OUTPUT)
                    {
                        sb.append(ChatColor.RED);
                        sb.append(output.getLabel());
                        sb.append(ChatColor.GOLD);
                        sb.append(" | ");
                    }

                    // Delete the last " | ".
                    sb.delete(sb.length() - 2, sb.length());
                    sender.sendMessage(sb.toString());
                    return;
                }

                user.getInventoryHeuristicsData().trainedPattern = neuralPattern;
                user.getInventoryHeuristicsData().trainingLabel = label;

                final String messageString = ChatColor.GOLD + "[HEURISTICS] Training " + ChatColor.RED + patternName +
                                             ChatColor.GOLD + " | Player: " + ChatColor.RED + trainingPlayer.getName() +
                                             ChatColor.GOLD + " | Output: " + ChatColor.RED + label;

                sender.sendMessage(messageString);
                // .substring(13) to remove the [HEURISTICS] label.
                VerboseSender.sendVerboseMessage(ChatColor.stripColor(messageString).substring(13));
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
                };
    }

    @Override
    protected List<String> getTabPossibilities()
    {
        return ImmutableList.of("vanilla", "cheating");
    }
}
