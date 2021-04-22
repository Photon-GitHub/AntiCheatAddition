package de.photon.aacadditionpro.util.violationlevels;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.util.violationlevels.threshold.Threshold;
import de.photon.aacadditionpro.util.violationlevels.threshold.ThresholdManagement;
import de.photon.aacadditionproold.AACAdditionPro;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public abstract class ViolationManagement
{
    /**
     * The module id of the handler
     */
    protected final Module module;

    /**
     * A {@link List} of {@link Threshold}s which is guaranteed to be sorted.
     */
    protected final ThresholdManagement thresholds;

    public static Flag flagFromPlayer(Player player)
    {
        return new Flag(player);
    }

    public static Flag flagFromPlayers(Collection<Player> players)
    {
        return new Flag(players);
    }

    /**
     * Flags a {@link Player} according to the options set in the {@link Flag}.
     */
    public abstract void flag(final Flag flag);

    /**
     * @param uuid the {@link UUID} of the {@link Player} whose vl should be returned.
     *
     * @return the vl of the given uuid.
     */
    public abstract int getVL(final UUID uuid);

    /**
     * Sets the vl of a player.
     *
     * @param player the {@link Player} whose vl should be set
     * @param newVl  the new vl of the player.
     */
    public abstract void setVL(final Player player, final int newVl);

    /**
     * Adds an {@link Integer} to the vl of a player. The number can be negative, this will decrease the vl then.
     *
     * @param player the {@link Player} whose vl should be set
     * @param vl     the vl to be added to the current vl.
     */
    protected abstract void addVL(final Player player, final int vl);

    /**
     * Used to execute the commands that are defined in the config section CHECK_NAME.thresholds
     *
     * @param player the {@link Player} that should be punished and that should be used to apply the placeholders
     * @param fromVl the last vl of the player before the addition and the searching-range for command.
     */
    protected void punishPlayer(Player player, int fromVl, int toVl)
    {
        // Only schedule the command execution if the plugin is loaded and when we do not use AAC's feature handling.
        if (AACAdditionPro.getInstance().isLoaded() && AACAdditionPro.getInstance().getAacapi() == null) {
            this.thresholds.executeThresholds(fromVl, toVl, Collections.singleton(player));
        }
    }

    /**
     * Has options for the flagging process.
     * {@link Flag} contains the player
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    protected static class Flag
    {
        protected Player player;
        protected Collection<Player> team;
        protected int addedVl = 1;
        private int cancelVl = -1;
        private Runnable onCancel = null;
        private Runnable eventNotCancelled = null;

        private Flag(Player player)
        {
            this.player = player;
        }

        private Flag(Collection<Player> team)
        {
            this.team = team;
        }

        public Flag setAddedVl(int addedVl)
        {
            Preconditions.checkArgument(cancelVl >= 1, "Tried to add no or negative vl in flag.");
            this.addedVl = addedVl;
            return this;
        }

        public Flag setCancelAction(int cancelVl, Runnable onCancel)
        {
            Preconditions.checkArgument(cancelVl >= 0, "Set negative cancel vl in flag.");
            this.cancelVl = cancelVl;
            this.onCancel = Preconditions.checkNotNull(onCancel, "Tried to set null onCancel action in flag.");
            return this;
        }

        public Flag setEventNotCancelledAction(Runnable eventNotCancelled)
        {
            this.eventNotCancelled = Preconditions.checkNotNull(eventNotCancelled, "Tried to set null eventNotCancelled action in flag.");
            return this;
        }

        /**
         * This method will execute both the
         */
        public void executeRunnablesIfNeeded(int currentVl)
        {
            if (this.cancelVl >= 0 && currentVl >= this.cancelVl) this.onCancel.run();
            if (this.eventNotCancelled != null) this.eventNotCancelled.run();
        }
    }
}
