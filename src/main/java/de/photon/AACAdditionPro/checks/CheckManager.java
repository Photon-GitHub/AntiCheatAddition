package de.photon.AACAdditionPro.checks;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.Manager;
import de.photon.AACAdditionPro.checks.subchecks.AutoFish;
import de.photon.AACAdditionPro.checks.subchecks.AutoPotion;
import de.photon.AACAdditionPro.checks.subchecks.BlindnessSprint;
import de.photon.AACAdditionPro.checks.subchecks.EqualRotation;
import de.photon.AACAdditionPro.checks.subchecks.Esp;
import de.photon.AACAdditionPro.checks.subchecks.Fastswitch;
import de.photon.AACAdditionPro.checks.subchecks.Freecam;
import de.photon.AACAdditionPro.checks.subchecks.InventoryChat;
import de.photon.AACAdditionPro.checks.subchecks.InventoryHeuristics;
import de.photon.AACAdditionPro.checks.subchecks.InventoryHit;
import de.photon.AACAdditionPro.checks.subchecks.InventoryMove;
import de.photon.AACAdditionPro.checks.subchecks.InventoryRotation;
import de.photon.AACAdditionPro.checks.subchecks.KillauraEntity;
import de.photon.AACAdditionPro.checks.subchecks.MultiInteraction;
import de.photon.AACAdditionPro.checks.subchecks.Pingspoof;
import de.photon.AACAdditionPro.checks.subchecks.Scaffold;
import de.photon.AACAdditionPro.checks.subchecks.Skinblinker;
import de.photon.AACAdditionPro.checks.subchecks.Teaming;
import de.photon.AACAdditionPro.checks.subchecks.Tower;
import de.photon.AACAdditionPro.checks.subchecks.clientcontrol.BetterSprintingControl;
import de.photon.AACAdditionPro.checks.subchecks.clientcontrol.FiveZigControl;
import de.photon.AACAdditionPro.checks.subchecks.clientcontrol.ForgeControl;
import de.photon.AACAdditionPro.checks.subchecks.clientcontrol.LabyModControl;
import de.photon.AACAdditionPro.checks.subchecks.clientcontrol.LiteloaderControl;
import de.photon.AACAdditionPro.checks.subchecks.clientcontrol.SchematicaControl;
import de.photon.AACAdditionPro.checks.subchecks.clientcontrol.WorldDownloaderControl;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import org.bukkit.Bukkit;

public final class CheckManager extends Manager<AACAdditionProCheck>
{
    public static final CheckManager checkManagerInstance = new CheckManager();

    private CheckManager()
    {
        super(
                // ClientControl
                new BetterSprintingControl(),
                new FiveZigControl(),
                new ForgeControl(),
                new LabyModControl(),
                new LiteloaderControl(),
                new SchematicaControl(),
                new WorldDownloaderControl(),

                // Normal checks
                new AutoFish(),
                new AutoPotion(),
                new BlindnessSprint(),
                new EqualRotation(),
                new Esp(),
                new Fastswitch(),
                new Freecam(),
                new InventoryChat(),
                new InventoryHeuristics(),
                new InventoryHit(),
                new InventoryMove(),
                new InventoryRotation(),
                new KillauraEntity(),
                new MultiInteraction(),
                new Pingspoof(),
                new Scaffold(),
                new Skinblinker(),
                new Teaming(),
                new Tower()
             );
    }

    @Override
    protected void registerObject(final AACAdditionProCheck object)
    {
        final String verboseName = object.getName().replace(".", ": ");
        try {
            // Enabled in the config
            if (AACAdditionPro.getInstance().getConfig().getBoolean(object.getAdditionHackType().getConfigString() + ".enabled")) {

                // Supports the current server version
                if (object.getSupportedVersions().contains(AACAdditionPro.getInstance().getServerVersion())) {
                    // Enable
                    object.enable();
                    VerboseSender.sendVerboseMessage(verboseName + " has been enabled.", true, false);
                } else {
                    // Auto-Disable as of the wrong server version
                    Bukkit.getScheduler().scheduleSyncDelayedTask(AACAdditionPro.getInstance(), () -> this.managedObjects.remove(object), 1L);
                    VerboseSender.sendVerboseMessage(verboseName + " is not compatible with the server-version.", true, false);
                }
            } else {
                // Disable as it was chosen so in the config
                Bukkit.getScheduler().scheduleSyncDelayedTask(AACAdditionPro.getInstance(), () -> this.managedObjects.remove(object), 1L);
                VerboseSender.sendVerboseMessage(verboseName + " was chosen not to be enabled.", true, false);
            }
        } catch (final Exception e) {
            // Error handling
            VerboseSender.sendVerboseMessage(verboseName + " could not be registered.", true, true);
            e.printStackTrace();
        }
    }

    /**
     * Enables or disables a check in runtime
     *
     * @param additionHackType the {@link AdditionHackType} of the check that should be disabled.
     */
    public void setStateOfCheck(final AdditionHackType additionHackType, final boolean state)
    {
        for (final AACAdditionProCheck check : managedObjects) {
            if (check.getAdditionHackType() == additionHackType) {
                // The message that will be printed in the logs / console
                String message = "Check " + check.getName() + "has been ";

                // Should it be enabled or disabled
                if (state) {
                    check.enable();
                    message += "enabled.";
                } else {
                    check.disable();
                    message += "disabled.";
                }

                // Send / log the message
                VerboseSender.sendVerboseMessage(message);
                // Only one check can have an AdditionHackType, therefore this loop can be breaked.
                break;
            }
        }
    }

    public static void startCheckManager() {}
}
