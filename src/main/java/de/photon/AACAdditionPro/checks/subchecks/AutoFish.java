package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import de.photon.AACAdditionPro.util.multiversion.ServerVersion;
import de.photon.AACAdditionPro.util.storage.management.ViolationLevelManagement;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AutoFish implements Listener, ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 3600);

    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancel_vl;

    /**
     * The different parts of the check:
     * [0] == inhuman_reaction
     * [1] == consistency
     */
    private final boolean[] parts = new boolean[2];

    // Inhuman reaction
    @LoadFromConfiguration(configPath = ".parts.inhuman_reaction.fishing_milliseconds")
    private int fishing_milliseconds;

    // Consistency
    @LoadFromConfiguration(configPath = ".parts.consistency.violation_offset")
    private int violation_offset;

    @LoadFromConfiguration(configPath = ".parts.consistency.maximum_fails")
    private int maximum_fails;

    @EventHandler
    public void on(final PlayerFishEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // User valid and not bypassed
        if (User.isUserInvalid(user))
        {
            return;
        }

        switch (event.getState())
        {
            case FISHING:
                // Get the fails
                final int fails = user.getFishingData().failedCounter;
                user.getFishingData().failedCounter = 0;

                // Only check if consistency is enabled
                if (parts[1] &&
                    // Not too many failed attempts in between (afk fish farm false positives)
                    fails <= maximum_fails &&
                    // If the timestamp is 0 do not check (false positives)
                    user.getFishingData().getTimeStamp(1) != 0 &&
                    // Add the delta to the consistencyBuffer of the user.
                    user.getFishingData().bufferConsistencyData())
                {
                    // Enough data, now checking
                    // Calculating the average
                    final double average = user.getFishingData().consistencyBuffer.average();

                    // Partially clear the buffer already in the loop to improve performance (instead of get())
                    final double minValue = user.getFishingData().consistencyBuffer.min();
                    final double maxValue = user.getFishingData().consistencyBuffer.max();

                    final double maxOffset = Math.max(MathUtils.offset(minValue, average), MathUtils.offset(maxValue, average));

                    // Certainly in cheating range (higher values terminate the method in the loop)
                    // First string is the average time, second one the maximum offset
                    final String[] verboseStrings = new String[]{
                            String.valueOf(average),
                            String.valueOf(maxOffset)
                    };

                    for (int i = 0; i < verboseStrings.length; i++)
                    {
                        verboseStrings[i] = verboseStrings[i].substring(0, Math.min(verboseStrings.length, 7));
                    }

                    VerboseSender.sendVerboseMessage("AutoFish-Verbose | Player: " + user.getPlayer().getName() + " average time: " + verboseStrings[0] + " | maximum offset: " + verboseStrings[1]);
                    // assert violation_offset - maxOffset > 0 as of the termination in the loop above.
                    vlManager.flag(event.getPlayer(), (int) Math.max(Math.ceil((violation_offset - maxOffset) * 0.6), 15), cancel_vl, () -> event.setCancelled(true), () -> {});
                }
                break;
            case CAUGHT_ENTITY:
            case CAUGHT_FISH:
                // Only check if inhuman reaction is enabled
                if (parts[0])
                {
                    // Too few time has passed since the fish bit.
                    if (user.getFishingData().recentlyUpdated(fishing_milliseconds))
                    {

                        // Get the correct amount of vl.
                        // vl 6 is the maximum.
                        // Points = {{0, 1}, {8, 0}}
                        // Function: 1 - 0.125x
                        for (byte b = 5; b > 0; b--)
                        {
                            if (user.getFishingData().recentlyUpdated((long) (1 - 0.125 * b) * fishing_milliseconds))
                            {
                                // Flag for vl = b + 1 because there would otherwise be a "0-vl"
                                vlManager.flag(event.getPlayer(), b + 1, cancel_vl, () -> event.setCancelled(true), () -> {});
                                break;
                            }
                        }
                    }

                    // Reset the bite-timestamp to be ready for the next one
                    user.getFishingData().nullifyTimeStamp();

                    // Consistency check
                    user.getFishingData().updateTimeStamp(1);
                }
                break;
            // No consistency when not fishing / failed fishing
            case IN_GROUND:
            case FAILED_ATTEMPT:
                user.getFishingData().nullifyTimeStamp(1);
                user.getFishingData().failedCounter++;
                break;
            case BITE:
                user.getFishingData().updateTimeStamp();
                break;
        }
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.AUTO_FISH;
    }

    @Override
    public void subEnable()
    {
        // Parts
        this.parts[0] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".parts.inhuman_reaction.enabled");
        this.parts[1] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".parts.consistency.enabled");

        VerboseSender.sendVerboseMessage("AutoFish-Parts: inhuman reaction: " + parts[0] + " | consistency: " + parts[1]);
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public Set<ServerVersion> getSupportedVersions()
    {
        return new HashSet<>(Arrays.asList(ServerVersion.MC110, ServerVersion.MC111, ServerVersion.MC112));
    }
}
