package de.photon.AACAdditionPro.command.subcommands;

import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.command.InternalCommand;
import org.bukkit.command.CommandSender;

import java.util.Queue;

public class TabListRemoveCommand extends InternalCommand
{
    public TabListRemoveCommand()
    {
        super("tablistremove", InternalPermission.TABLISTREMOVE, false, (byte) 2, (byte) 3);
    }


    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        
    }

    @Override
    protected String[] getCommandHelp()
    {
        return new String[]{
                "Removes a player from the tablist of a player and readds him after a certain time.",
                "Without a provided timeframe the command will add the player back to the tablist immediately.",
                "Syntax: /aacadditionpro tablistremove <checkedplayer> <removedplayer> [<time>]"
        };
    }

    @Override
    protected String[] getTabPossibilities()
    {
        return new String[0];
    }
}
