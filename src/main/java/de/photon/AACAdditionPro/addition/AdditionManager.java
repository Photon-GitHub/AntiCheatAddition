package de.photon.AACAdditionPro.addition;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.Manager;
import de.photon.AACAdditionPro.addition.additions.LogBot;
import de.photon.AACAdditionPro.addition.additions.PerHeuristicCommands;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import org.bukkit.Bukkit;

public final class AdditionManager extends Manager<Addition>
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

    @Override
    protected void registerObject(final Addition object)
    {
        try {
            if (AACAdditionPro.getInstance().getConfig().getBoolean(object.getConfigString() + ".enabled")) {
                if (object.getSupportedVersions().contains(AACAdditionPro.getInstance().getServerVersion())) {
                    object.enable();
                    VerboseSender.sendVerboseMessage(object.getConfigString() + " has been enabled.", true, false);
                } else {
                    Bukkit.getScheduler().scheduleSyncDelayedTask(AACAdditionPro.getInstance(), () -> this.getManagedObjects().remove(object), 1L);
                    VerboseSender.sendVerboseMessage(object.getConfigString() + " is not compatible with the server-version.", true, false);
                }
            } else {
                Bukkit.getScheduler().scheduleSyncDelayedTask(AACAdditionPro.getInstance(), () -> this.getManagedObjects().remove(object), 1L);
                VerboseSender.sendVerboseMessage(object.getConfigString() + " was chosen not to be enabled.", true, false);
            }
        } catch (final Exception e) {
            VerboseSender.sendVerboseMessage(object.getConfigString() + " could not be registered.", true, true);
            e.printStackTrace();
        }
    }

    public static void startAdditionManager(){}
}