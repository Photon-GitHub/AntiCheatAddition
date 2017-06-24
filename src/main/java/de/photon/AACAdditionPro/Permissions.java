package de.photon.AACAdditionPro;

import org.bukkit.entity.Player;

public enum Permissions
{
    VERBOSE("aac.verbose"),
    INFO("aacadditionpro.info"),
    NEURAL("aacadditionpro.neural"),
    NEURAL_TRAIN("aacadditionpro.neural.train"),
    NEURAL_CHECK("aacadditionpro.neural.check");

    private final String realPermission;

    Permissions(final String realPermission)
    {
        this.realPermission = realPermission;
    }

    /**
     * This checks if a {@link Player} has a certain permission.
     *
     * @param player     the {@link Player} who should be checked
     * @param permission the permission that he should be checked for
     *
     * @return true if the player has the permission, otherwise false
     */
    public boolean hasPermission(final Player player, final Permissions permission)
    {
        return player.hasPermission(permission.realPermission);
    }
}