package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
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

public class AutoFish implements Listener, AACAdditionProCheck
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getAdditionHackType(), 3600);

    // Must be sorted from high to low
    private static final float[] autofishPercentageThresholds = {
            1F,
            0.68F,
            0.42F
    };

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

    @LoadFromConfiguration(configPath = ".parts.consistency.weight")
    private int weight;

    @EventHandler
    public void on(final PlayerFishEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // User valid and not bypassed
        if (AACAdditionProCheck.isUserInvalid(user)) {
            return;
        }

        switch (event.getState()) {
            case FISHING:
                // Only check when consistency is enabled
                if (!parts[1] ||
                    // If the timestamp is 0 do not check (false positives)
                    user.getFishingData().getTimeStamp(1) == 0)
                {
                    return;
                }

                // Add the delta to the consistencyBuffer of the user.
                if (user.getFishingData().bufferConsistencyData()) {
                    // Enough data, now checking
                    // Calculating the average
                    final double average = user.getFishingData().consistencyBuffer.average();

                    // Test if the average is exceeded by the violation_offset
                    boolean cheating = true;

                    // Partially clear the buffer already in the loop to improve performance (instead of get())
                    while (!user.getFishingData().consistencyBuffer.isEmpty()) {
                        // Get the last element to make the ArrayList-remove as performant as possible
                        double deltaTime = user.getFishingData().consistencyBuffer.get(user.getFishingData().consistencyBuffer.size() - 1);

                        // Not in range anymore -> not consistent enough for a flag.
                        if (!MathUtils.isInRange(deltaTime, average, violation_offset)) {
                            cheating = false;

                            // Clear the rest of the buffer that was not cleared here so the method is not called when the Buffer is emptied by this while-loop
                            // Clear for a new run.
                            user.getFishingData().consistencyBuffer.clear();
                            break;
                        }
                    }

                    if (cheating) {
                        vlManager.flag(event.getPlayer(), this.weight, cancel_vl, () -> event.setCancelled(true), () -> {});
                    }
                }
                break;
            case CAUGHT_ENTITY:
            case CAUGHT_FISH:
                // Only check if inhuman reaction is enabled
                if (parts[0]) {
                    // Too few time has passed since the fish bit.
                    if (user.getFishingData().recentlyUpdated(fishing_milliseconds)) {

                        // Get the correct amount of vl.
                        for (byte b = (byte) (autofishPercentageThresholds.length - 1); b >= 0; b--) {
                            if (user.getFishingData().recentlyUpdated((long) (autofishPercentageThresholds[b] * fishing_milliseconds))) {
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
                break;
            case BITE:
                user.getFishingData().updateTimeStamp();
                break;
        }
    }

    @Override
    public AdditionHackType getAdditionHackType()
    {
        return AdditionHackType.AUTO_FISH;
    }

    @Override
    public void subEnable()
    {
        // Parts
        this.parts[0] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getAdditionHackType().getConfigString() + ".parts.inhuman_reaction.enabled");
        this.parts[1] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getAdditionHackType().getConfigString() + ".parts.consistency.enabled");

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
