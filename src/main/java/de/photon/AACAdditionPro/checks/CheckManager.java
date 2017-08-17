package de.photon.AACAdditionPro.checks;

import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.Module;
import de.photon.AACAdditionPro.ModuleManager;
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

public final class CheckManager extends ModuleManager
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

    /**
     * Gets a check from its {@link AdditionHackType}.
     *
     * @param additionHackType the {@link AdditionHackType} of the check that should be found
     *
     * @return the check if it was found
     *
     * @throws IllegalArgumentException if the provided {@link AdditionHackType} parameter is not used in a check
     */
    public AACAdditionProCheck getCheck(final AdditionHackType additionHackType)
    {
        for (final Module module : this) {
            // No problem to cast here as only AACAdditionProChecks go here.
            final AACAdditionProCheck check = (AACAdditionProCheck) module;
            if (check.getAdditionHackType() == additionHackType) {
                return check;
            }
        }
        throw new IllegalArgumentException("The AdditionHackType: " + additionHackType.name() + " is not used in any registered check (is the server version compatible with it?).");
    }

    /**
     * Enables or disables a check in runtime
     *
     * @param additionHackType the {@link AdditionHackType} of the check that should be disabled.
     */
    public void setStateOfCheck(final AdditionHackType additionHackType, final boolean state)
    {
        final AACAdditionProCheck check = this.getCheck(additionHackType);

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
    }

    public static void startCheckManager() {}
}
