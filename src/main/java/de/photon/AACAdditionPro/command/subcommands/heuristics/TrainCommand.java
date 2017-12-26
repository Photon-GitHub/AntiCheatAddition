package de.photon.AACAdditionPro.command.subcommands.heuristics;

import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.checks.subchecks.InventoryHeuristics;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.command.subcommands.HeuristicsCommand;
import de.photon.AACAdditionPro.heuristics.OutputData;
import de.photon.AACAdditionPro.heuristics.Pattern;
import de.photon.AACAdditionPro.heuristics.TrainingData;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class TrainCommand extends InternalCommand
{
    public TrainCommand()
    {
        super("train", InternalPermission.NEURAL_TRAIN, false, (byte) 3, (byte) 4);
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
                throw new IllegalStateException("Two patterns with the same name exist.");
            }
            else
            {
                final Player trainingPlayer = Bukkit.getServer().getPlayer(arguments.remove());

                if (trainingPlayer == null)
                {
                    sender.sendMessage(playerNotFoundMessage);
                    return;
                }

                final User user = UserManager.getUser(trainingPlayer.getUniqueId());

                // Not bypassed
                if (user == null)
                {
                    sender.sendMessage(playerNotFoundMessage);
                    return;
                }

                final String output = arguments.remove().toUpperCase();

                if (output.equals("VANILLA") || output.equals("CHEATING"))
                {
                    // The cycles argument is optional
                    final int cycles = arguments.isEmpty() ? 3 : Integer.valueOf(arguments.remove());
                    System.out.println("Player: " + trainingPlayer + " UUID: " + trainingPlayer.getUniqueId() + " output: " + output + " cycles: " + cycles);

                    user.getInventoryData().inventoryClicks.clear();
                    possiblePatterns.forEach(pattern -> pattern.addTrainingData(new TrainingData(trainingPlayer.getUniqueId(), new OutputData(output), cycles)));

                    final String messageString = "[HEURISTICS] Training " + patternName + " | Player: " + trainingPlayer.getName() + " | Output: " + output + " | Cycles: " + cycles;

                    sender.sendMessage(prefix + ChatColor.GOLD + messageString);
                    VerboseSender.sendVerboseMessage(messageString);
                }
                else
                {
                    sender.sendMessage(HeuristicsCommand.HEURISTICS_HEADER);
                    sender.sendMessage(ChatColor.GOLD + "Output \"" + ChatColor.RED + output + ChatColor.GOLD + "\"" + " is not allowed.");
                    sender.sendMessage(ChatColor.GOLD + "Allowed outputs: " + ChatColor.RED + "VANILLA" + ChatColor.GOLD + " | " + ChatColor.RED + "CHEATING");
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
        return new String[]{
                "Train a pattern with an example.",
                "Format: /aacadditionpro train <name of pattern> <player to learn from> <output> [<cycles>]"
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
