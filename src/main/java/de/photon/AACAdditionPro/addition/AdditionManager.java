package de.photon.AACAdditionPro.addition;

import de.photon.AACAdditionPro.ModuleManager;
import de.photon.AACAdditionPro.addition.additions.LogBot;
import de.photon.AACAdditionPro.addition.additions.PerHeuristicCommands;

public final class AdditionManager extends ModuleManager
{
    public final static AdditionManager additionManagerInstance = new AdditionManager();

    private AdditionManager()
    {
        super(
                // Additions
                new PerHeuristicCommands(),
                new LogBot()
             );
    }

    public static void startAdditionManager() {}
}