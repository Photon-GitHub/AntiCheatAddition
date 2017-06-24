package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.multiversion.ServerVersion;
import de.photon.AACAdditionPro.util.storage.management.Buffer;
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

    private int cancel_vl;

    /**
     * The different parts of the check:
     * [0] == inhuman_reaction
     * [1] == consistency
     */
    private final boolean[] parts = new boolean[2];

    // Inhuman reaction
    private int fishing_milliseconds;

    // Consistency
    private int violation_offset;
    private int weight;
    private Buffer<Long> consistencyData;

    @EventHandler
    public void on(final PlayerFishEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // User valid and not bypassed
        if (user == null || user.isBypassed()) {
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

                // Add the delta to the
                if (consistencyData.bufferObject(System.currentTimeMillis() - user.getFishingData().getTimeStamp(1))) {
                    // Enough data, now checking
                    // Calculating the average
                    double average = 0D;
                    for (final Long deltaTime : consistencyData) {
                        average += deltaTime;
                    }

                    average /= consistencyData.size();

                    // Test if the average is exceeded by the violation_offset
                    boolean legit = false;
                    for (final Long deltaTime : consistencyData) {
                        if (deltaTime > average - violation_offset && deltaTime < average + violation_offset) {
                            legit = true;
                            break;
                        }
                    }

                    // No legit -> flag
                    if (!legit) {
                        vlManager.flag(event.getPlayer(), this.weight, cancel_vl, () -> event.setCancelled(true), () -> {});
                    }
                }
                break;
            case CAUGHT_ENTITY:
            case CAUGHT_FISH:
                // Only check if inhuman reaction is enabled
                if (parts[0]) {
                    // THe normal reaction speed check
                    // In the HashMap (Detection)
                    if (user.getFishingData().recentlyUpdated(fishing_milliseconds)) {
                        if (user.getFishingData().recentlyUpdated((long) (0.42 * fishing_milliseconds))) {
                            vlManager.flag(event.getPlayer(), 3, cancel_vl, () -> event.setCancelled(true), () -> {});
                        } else if (user.getFishingData().recentlyUpdated((long) (0.68 * fishing_milliseconds))) {
                            vlManager.flag(event.getPlayer(), 2, cancel_vl, () -> event.setCancelled(true), () -> {});
                        } else {
                            vlManager.flag(event.getPlayer(), cancel_vl, () -> event.setCancelled(true), () -> {});
                        }
                    }
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
        // Cancel-VL
        this.cancel_vl = AACAdditionPro.getInstance().getConfig().getInt(this.getAdditionHackType().getConfigString() + ".cancel_vl");

        // Parts
        this.parts[0] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getAdditionHackType().getConfigString() + ".parts.inhuman_reaction.enabled");
        this.parts[1] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getAdditionHackType().getConfigString() + ".parts.consistency.enabled");

        VerboseSender.sendVerboseMessage("AutoFish-Parts: inhuman reaction: " + parts[0] + " | consistency: " + parts[1]);

        // Inhuman reaction
        this.fishing_milliseconds = AACAdditionPro.getInstance().getConfig().getInt(this.getAdditionHackType().getConfigString() + ".parts.inhuman_reaction.fishing_milliseconds");

        // Consistency
        this.violation_offset = AACAdditionPro.getInstance().getConfig().getInt(this.getAdditionHackType().getConfigString() + ".parts.consistency.violation_offset");
        this.weight = AACAdditionPro.getInstance().getConfig().getInt(this.getAdditionHackType().getConfigString() + ".parts.consistency.weight");
        // Create a buffer with the limit of consistency_events
        this.consistencyData = new Buffer<Long>(AACAdditionPro.getInstance().getConfig().getInt(this.getAdditionHackType().getConfigString() + ".parts.consistency.consistency_events"))
        {
            @Override
            public boolean verifyObject(final Long object)
            {
                return consistencyData.isEmpty() || object < consistencyData.peek() + (2 * violation_offset) && object > consistencyData.peek() - (2 * violation_offset);
            }
        };
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
