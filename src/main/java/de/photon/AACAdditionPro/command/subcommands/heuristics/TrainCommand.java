package de.photon.AACAdditionPro.command.subcommands.heuristics;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.checks.subchecks.InventoryHeuristics;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.heuristics.OutputData;
import de.photon.AACAdditionPro.heuristics.Pattern;
import de.photon.AACAdditionPro.heuristics.TrainingData;
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
        super("train", InternalPermission.NEURAL_TRAIN, (byte) 3);
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        if (AACAdditionPro.getInstance().getConfig().getBoolean("InventoryHeuristics.enabled"))
        {
            final Player p = Bukkit.getServer().getPlayer(arguments.remove());

            if (p == null)
            {
                sender.sendMessage(playerNotFoundMessage);
            }
            else
            {
                sender.sendMessage(prefix + ChatColor.GOLD + "[HEURISTICS] Training " + arguments.element().toUpperCase() + " Player: " + p.getName());
                VerboseSender.sendVerboseMessage("[HEURISTICS] Training " + arguments.element().toUpperCase() + "; Player: " + p.getName());

                final String patternName = arguments.remove();
                final Set<Pattern> possiblePatterns = InventoryHeuristics.getPATTERNS().stream().filter(pattern -> pattern.getName().equals(patternName)).collect(Collectors.toSet());

                if (possiblePatterns.isEmpty())
                {
                    sender.sendMessage(ChatColor.GOLD + "------" + ChatColor.DARK_RED + " Heuristics - Pattern " + ChatColor.GOLD + "------");
                    sender.sendMessage(ChatColor.GOLD + "Pattern \"" + patternName + "\"" + " could not be found.");
                }
                else if (possiblePatterns.size() > 1)
                {
                    throw new IllegalStateException("Two patterns with the same name exist.");
                }
                else
                {
                    String output = arguments.remove().toUpperCase();

                    if (output.equals("VANILLA") || output.equals("CHEATING"))
                    {
                        possiblePatterns.forEach(pattern -> pattern.setTrainingData(new TrainingData(p.getUniqueId(), new OutputData(output))));
                    }
                    else
                    {
                        sender.sendMessage(ChatColor.GOLD + "------" + ChatColor.DARK_RED + " Heuristics - Pattern " + ChatColor.GOLD + "------");
                        sender.sendMessage(ChatColor.GOLD + "Output \"" + output + "\"" + " is not allowed.");
                        sender.sendMessage(ChatColor.GOLD + "Allowed outputs: VANILLA | CHEATING");
                    }
                }
            }
        }
        else
        {
            sender.sendMessage(prefix + ChatColor.RED + "InventoryHeuristics is not loaded / enabled.");
        }
    }

    @Override
    protected String[] getCommandHelp()
    {
        return new String[]{"Train the Inventory-Heuristics with an example-player"};
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
