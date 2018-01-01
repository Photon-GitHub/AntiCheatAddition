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

            if (pattern == null)
            {
                sender.sendMessage(HeuristicsCommand.HEURISTICS_HEADER);
                sender.sendMessage(HeuristicsCommand.createPatternNotFoundMessage(patternName));
            }
            else
            {
                final String playerOrFinishArgument = arguments.remove();

                if (playerOrFinishArgument.equalsIgnoreCase("finish"))
                {
                    pattern.train();

                    sender.sendMessage(HeuristicsCommand.HEURISTICS_HEADER);
                    final String messageString = ChatColor.GOLD + "Training of pattern " + ChatColor.RED + pattern.getName() + ChatColor.GOLD + " finished.";
                    VerboseSender.sendVerboseMessage(ChatColor.stripColor(messageString));
                }
                else
                {
                    final Player trainingPlayer = Bukkit.getServer().getPlayer(playerOrFinishArgument);

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
                        user.getInventoryData().inventoryClicks.clear();
                        pattern.getTrainingDataSet().add(new TrainingData(trainingPlayer.getUniqueId(), new OutputData(output)));

                        final String messageString = ChatColor.GOLD + "[HEURISTICS] Training " + ChatColor.RED + patternName +
                                                     ChatColor.GOLD + " | Player: " + ChatColor.RED + trainingPlayer.getName() +
                                                     ChatColor.GOLD + " | Output: " + ChatColor.RED + output;

                        sender.sendMessage(prefix + messageString);
                        VerboseSender.sendVerboseMessage(ChatColor.stripColor(messageString));
                    }
                    else
                    {
                        sender.sendMessage(HeuristicsCommand.HEURISTICS_HEADER);
                        sender.sendMessage(ChatColor.GOLD + "Output \"" + ChatColor.RED + output + ChatColor.GOLD + "\"" + " is not allowed.");
                        sender.sendMessage(ChatColor.GOLD + "Allowed outputs: " + ChatColor.RED + "VANILLA" + ChatColor.GOLD + " | " + ChatColor.RED + "CHEATING");
                    }
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
