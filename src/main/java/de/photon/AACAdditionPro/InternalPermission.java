package de.photon.AACAdditionPro;

import org.bukkit.permissions.Permissible;

public enum InternalPermission
{
    BYPASS("aac.bypass"),
    VERBOSE("aac.verbose"),
    INFO("aacadditionpro.info"),
    ENTITYCHECK("aacadditionpro.entitycheck"),
    TABLISTREMOVE("aacadditionpro.tablistremove"),
    NEURAL("aacadditionpro.neural"),
    NEURAL_CREATE("aacadditionpro.neural.create"),
    NEURAL_TRAIN("aacadditionpro.neural.train");

    private final String realPermission;

    InternalPermission(final String realPermission)
    {
        this.realPermission = realPermission;
    }

    /**
     * This checks if a {@link Permissible} has a certain permission.
     *
     * @param permissible the {@link Permissible} who should be checked
     * @param permission  the permission that he should be checked for
     *
     * @return true if the player has the permission or is op, otherwise false
     */
    public static boolean hasPermission(final Permissible permissible, final InternalPermission permission)
    {
        return permission == null || permissible.isOp() || permissible.hasPermission(permission.realPermission);
    }
}